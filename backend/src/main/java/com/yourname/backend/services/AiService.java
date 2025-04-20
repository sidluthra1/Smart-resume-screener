package com.yourname.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths; // Keep this import if needed elsewhere, not strictly needed for the fix

@Service
public class AiService {
    private final ObjectMapper MAPPER = new ObjectMapper();
    // adjust python path & script location as needed
    private static final String PYTHON = "python3";
    // Ensure this relative path is correct from where Spring Boot runs (usually project root)
    private static final String SCRIPT = "ai/semantic_matcher.py"; // Adjusted relative path assumption

    public double scoreResume(String resumePath, String jobPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                PYTHON, SCRIPT,
                resumePath, jobPath
        );
        // Ensure the working directory is correct if the script path is relative
        // Example: pb.directory(new File("../")); // Or wherever the parent of 'ai' and 'backend' is

        pb.redirectErrorStream(true); // Good for debugging script errors
        Process proc = pb.start();

        // Use try-with-resources for BufferedReader
        try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String jsonOutput = in.readLine();
            if (jsonOutput == null || jsonOutput.trim().isEmpty()) {
                // Handle cases where the script might not output anything or errors out silently
                try (BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                    String errorLine;
                    StringBuilder errorOutput = new StringBuilder();
                    while ((errorLine = err.readLine()) != null) {
                        errorOutput.append(errorLine).append("\n");
                    }
                    throw new RuntimeException("AI script produced no output. Error stream: " + errorOutput.toString());
                } catch (IOException e) {
                    throw new RuntimeException("AI script produced no output and failed to read error stream.", e);
                }
            }

            JsonNode node = MAPPER.readTree(jsonOutput);

            // --- FIX IS HERE ---
            // Check for the correct key "Match Score" provided by the Python script
            if (node.has("Match Score")) {
                // Get the value associated with the correct key "Match Score"
                return node.get("Match Score").asDouble();
            } else {
                // The error message now correctly reflects what was received
                throw new RuntimeException("AI script JSON output missing 'Match Score' key. Received: " + jsonOutput);
            }
            // --- END OF FIX ---

        } finally {
            // Ensure the process terminates and resources are cleaned up
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                // Log or handle non-zero exit code from Python script if needed
                System.err.println("Python script exited with code: " + exitCode);
                // Consider throwing an exception here too if a non-zero exit code is always an error
            }
            proc.destroy(); // Forcefully destroy if it hasn't terminated
        }
    }
}