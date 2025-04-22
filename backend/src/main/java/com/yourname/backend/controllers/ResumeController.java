package com.yourname.backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resume")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {

    private final StorageService storageService;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Path to your Python executable, e.g. /usr/bin/python3 or just "python3" */
    @Value("${ai.python-executable:python3}")
    private String pythonExecutable;

    /** Relative path (from your working dir) to ResumeParser.py */
    @Value("${ai.parser-script:src/main/python/ResumeParser.py}")
    private String parserScript;

    @Autowired
    public ResumeController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Uploads a resume and returns the parsed JSON from ResumeParser.py
     */
    @PostMapping(path = "/parse",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> parse(@RequestParam("file") @NotNull MultipartFile file)
            throws IOException, InterruptedException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(mapper.createObjectNode().put("error", "File is empty"));
        }

        // 1) Save to disk
        String resumePath = storageService.store(file);

        // 2) Invoke the Python parser
        String stdout = runParser(resumePath);

        // 3) Parse JSON
        JsonNode parsed = mapper.readTree(stdout);

        return ResponseEntity.ok(parsed);
    }

    /**
     * Calls your ResumeParser.py script with the given resume path and captures stdout.
     */
    private String runParser(String resumePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                parserScript,
                resumePath
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // read both stdout & stderr
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exit = proc.waitFor();
        if (exit != 0) {
            throw new RuntimeException(
                    "ResumeParser.py exited with code " + exit + " and output:\n" + output
            );
        }

        // take last non-empty line as the JSON
        String[] lines = output.trim().split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        throw new RuntimeException("ResumeParser.py returned no JSON output");
    }
}
