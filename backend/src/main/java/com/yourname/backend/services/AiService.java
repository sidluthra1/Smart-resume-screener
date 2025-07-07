package com.yourname.backend.services;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    @Value("${python.scorer}")
    private String SCORER_SCRIPT;

    @Autowired
    private OpenAiHelper openAiHelper;

    public static record ScoreBundle(
            double finalScore,
            double semanticScore,
            double skillsScore,
            double educationScore,
            double experienceScore,
            double overlap,   // ← NEW: raw semantic-matcher overlap %
            double llmScore   // ← NEW: OpenAI score
    ) { }

    /**
     * Runs the scorer pipeline and returns all component scores.
     *
     * @param resumePlainTxt   plain-text resume
     * @param jobPlainTxt      plain-text JD
     * @param parsedResumeJson JSON from ResumeParser.py
     * @param parsedJobJson    JSON from JobDescriptionParser.py
     * @param overlapScore     % overlap already produced by semantic_matcher.py
     */
    public ScoreBundle scoreResume(
            String resumePlainTxt,
            String jobPlainTxt,
            String parsedResumeJson,
            String parsedJobJson,
            double overlapScore
    ) throws IOException, InterruptedException {

        double llmScore = openAiHelper.compareResumeAndJob(parsedResumeJson, parsedJobJson);
        log.debug("OpenAI returned LLMscore={}", llmScore);
        ObjectNode root = mapper.createObjectNode();
        root.put("Overlap",   overlapScore);
        root.put("LLMscore",  llmScore);
        root.put("resume_text", resumePlainTxt);
        root.put("job_text",    jobPlainTxt);
        root.set("resume_json", mapper.readTree(parsedResumeJson));
        root.set("job_json",    mapper.readTree(parsedJobJson));
        String input = mapper.writeValueAsString(root);

        ProcessBuilder pb = new ProcessBuilder(PYTHON, SCORER_SCRIPT);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
            w.write(input);
        }

        String lastLine = null;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) lastLine = line;
        }
        if (lastLine == null) lastLine = "{}";

        int exit = p.waitFor();
        log.debug("score_resumes.py exited with code {}", exit);
        if (exit != 0) {
            log.error("Scorer failed. Output was:\n{}", lastLine);
            throw new RuntimeException("Scorer script failed – see logs.");
        }

        JsonNode n = mapper.readTree(lastLine);
        double finalScore      = n.get("FinalScore").asDouble();
        double semanticScore   = n.get("SemanticScore").asDouble();
        double skillsScore     = n.get("SkillsScore").asDouble();
        double educationScore  = n.get("EducationScore").asDouble();
        double experienceScore = n.get("ExperienceScore").asDouble();

        log.info("Final blended score returned: {}", finalScore);

        return new ScoreBundle(
                finalScore,
                semanticScore,
                skillsScore,
                educationScore,
                experienceScore,
                overlapScore,
                llmScore
        );
    }
}
