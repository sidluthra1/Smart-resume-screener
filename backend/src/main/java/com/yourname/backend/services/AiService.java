package com.yourname.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;

@Service
public class AiService {
    private final ObjectMapper MAPPER = new ObjectMapper();
    // adjust python path & script location as needed
    private static final String PYTHON = "python3";
    private static final String SCRIPT = "../ai/semantic_matcher.py";

    public double scoreResume(String resumePath, String jobPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                PYTHON, SCRIPT,
                resumePath, jobPath
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String json = in.readLine();
            JsonNode node = MAPPER.readTree(json);
            if (node.has("score")) {
                return node.get("score").asDouble();
            } else {
                throw new RuntimeException("AI error: " + node);
            }
        } finally {
            proc.waitFor();
        }
    }
}
