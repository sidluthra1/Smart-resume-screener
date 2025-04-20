package com.yourname.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException; // Import this
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets; // Import StandardCharsets
import java.nio.file.Paths; // Can be useful for path manipulation if needed
import java.util.stream.Collectors; // Import Collectors

@Service
public class AiService {
    private final ObjectMapper MAPPER = new ObjectMapper();
    // Add a logger for better debugging
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    // --- Configuration ---
    // IMPORTANT: Replace this path with the actual output of `which python3`
    //            when your venv is activated in the terminal.
    // Example path shown for macOS, adjust if needed for Windows (\Scripts\python.exe)
    private static final String PYTHON = "/Users/siddarthluthra/Desktop/Smart Resume Screener/venv/bin/python3";

    // Relative path from the backend working directory to the script
    // (Since you moved 'ai' inside 'backend')
    private static final String SCRIPT = "ai/semantic_matcher.py";
    // --- End Configuration ---

    public double scoreResume(String resumePath, String jobPath) throws IOException, InterruptedException {

        // Ensure the configured Python path actually exists (optional sanity check)
        File pythonExecutable = new File(PYTHON);
        if (!pythonExecutable.exists()) {
            log.error("Configured Python executable not found at: {}", PYTHON);
            throw new FileNotFoundException("Python executable not found at configured path: " + PYTHON);
        }

        ProcessBuilder pb = new ProcessBuilder(
                PYTHON, // Use the absolute path to venv python
                SCRIPT, // Use the relative path to the script
                resumePath, // Absolute path from StorageService
                jobPath // Absolute path from Files.createTempFile
        );

        // Log the command being executed
        log.info("Executing AI script command: {}", String.join(" ", pb.command()));

        // Combine stdout and stderr so we capture everything in one stream
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        String output;
        // Read the entire output stream from the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        // Wait for the process to complete and check the exit code *first*
        int exitCode = proc.waitFor();
        log.info("Python script finished with exit code: {}", exitCode);

        // Handle script execution failure
        if (exitCode != 0) {
            // Log the captured output which likely contains the error message
            log.error("Python script failed with exit code {}. Captured output:\n---\n{}\n---", exitCode, output);
            throw new RuntimeException("AI script execution failed (exit code " + exitCode + "). Check backend logs for Python error details.");
        }

        // Handle case where script succeeded but produced no output
        if (output == null || output.trim().isEmpty()) {
            log.error("Python script exited successfully (code 0) but produced no output.");
            throw new RuntimeException("AI script produced no output.");
        }

        // Log the raw output received (useful for debugging)
        log.debug("Raw output received from Python script:\n---\n{}\n---", output);

        // --- FIX: Find the last non-empty line which should contain the JSON ---
        String jsonOutput = null;
        // Split the captured output into lines (\n or \r\n) after trimming start/end whitespace
        String[] lines = output.trim().split("\\r?\\n");
        // Iterate backwards through the lines
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim(); // Trim whitespace from the individual line
            if (!line.isEmpty()) {
                // Found the last non-empty line, assume it's the JSON
                jsonOutput = line;
                break;
            }
        }

        // Handle case where output contained only blank lines
        if (jsonOutput == null) {
            log.error("Python script output contained no non-empty lines despite exit code 0. Raw output:\n---\n{}\n---", output);
            throw new RuntimeException("AI script produced no parseable output line.");
        }
        // Log the specific line we are attempting to parse as JSON
        log.debug("Attempting to parse JSON from line: {}", jsonOutput);
        // --- END OF FIX ---

        // Now, attempt to parse the extracted JSON line
        try {
            // Use the extracted 'jsonOutput' string here
            JsonNode node = MAPPER.readTree(jsonOutput);

            // Check if the expected key exists in the parsed JSON
            if (node.has("Match Score")) {
                double score = node.get("Match Score").asDouble();
                log.info("Successfully parsed match score: {}", score);
                return score; // Success!
            } else {
                // Log error if the key is missing from the JSON line
                log.error("AI script JSON output line missing 'Match Score' key. Parsed line: {}", jsonOutput);
                throw new RuntimeException("AI script JSON output line missing 'Match Score' key. Received line: " + jsonOutput);
            }
        } catch (JsonProcessingException e) {
            // Catch errors specifically related to JSON parsing
            // Log the line we tried to parse and the original full output for context
            log.error("Failed to parse extracted Python script line as JSON. Line was:\n---\n{}\n---\nOriginal output was:\n---\n{}\n---", jsonOutput, output, e);
            throw new RuntimeException("Failed to parse AI script output line as JSON. See backend logs for details.", e);
        } finally {
            // Ensure the process resources are cleaned up
            proc.destroy();
            // Optional: Clean up the temporary job description file
            // try {
            //     Files.deleteIfExists(Paths.get(jobPath));
            // } catch (IOException e) {
            //     log.warn("Could not delete temporary job file: {}", jobPath, e);
            // }
        }
    }
}