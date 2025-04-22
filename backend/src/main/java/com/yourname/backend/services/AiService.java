package com.yourname.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /** absolute (or resolvable) path to the python interpreter in your venv */
    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    /** relative path inside backend to the new scorer */
    private static final String SCORER_SCRIPT = "src/main/python/score_resumes.py";

    /**
     * @param resumePlainTxt   plain‑text resume (already extracted)
     * @param jobPlainTxt      plain‑text job description
     * @param parsedResumeJson JSON string produced by ResumeParser.py
     * @param parsedJobJson    JSON string produced by JobDescriptionParser.py
     * @return blended 0‑100 score
     */
    public double scoreResume(
            String resumePlainTxt,
            String jobPlainTxt,
            String parsedResumeJson,
            String parsedJobJson
    ) throws IOException, InterruptedException {

        /* ------------------------------------------------------------------
           1) Build the single JSON payload that score_resumes.py expects
        ------------------------------------------------------------------ */
        ObjectNode root = mapper.createObjectNode();
        root.put("resume_text", resumePlainTxt);
        root.put("job_text",    jobPlainTxt);
        root.set("resume_json", mapper.readTree(parsedResumeJson));
        root.set("job_json",    mapper.readTree(parsedJobJson));
        String input = mapper.writeValueAsString(root);

        /* ------------------------------------------------------------------
           2) Launch the scoring script
        ------------------------------------------------------------------ */
        ProcessBuilder pb = new ProcessBuilder(PYTHON, SCORER_SCRIPT);
        pb.redirectErrorStream(true);               // merge stderr → stdout
        Process p = pb.start();

        /* ------------------------------------------------------------------
           3) Feed the JSON into the script’s STDIN
        ------------------------------------------------------------------ */
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
            w.write(input);
        }

        /* 4) Read everything the script prints to STDOUT */
        String output;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            output = r.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        int exit = p.waitFor();
        log.debug("score_resumes.py exited with code {}", exit);

        if (exit != 0) {
            log.error("Scorer failed. Output was:\n{}", output);
            throw new RuntimeException("Scorer script failed – see logs.");
        }

        /* ------------------------------------------------------------------
           5) Parse the JSON result and return the FinalScore
        ------------------------------------------------------------------ */
        JsonNode node = mapper.readTree(output);
        double finalScore = node.get("FinalScore").asDouble();
        log.info("Final blended score returned: {}", finalScore);
        return finalScore;
    }
}
