package com.yourname.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.*;
import java.net.URI;

/**
 * Thin wrapper around the OpenAI Chat Completion API that returns
 * a single 0‑100 score comparing a résumé and a job description.
 *
 * No external libraries required – uses java.net.http.
 */
@Service
public class OpenAiHelper {

    private static final Logger log = LoggerFactory.getLogger(OpenAiHelper.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${OPENAI_API_KEY}")
    private String apiKey;   // make sure this env var / property is set

    // You can move the model name to application.properties if you wish
    private static final String MODEL = "gpt-3.5-turbo";

    /**
     * @param resumeJson  JSON string from ResumeParser.py
     * @param jobJson     JSON string from JobDescriptionParser.py
     * @return            score 0‑100, or 0 if the call fails
     */
    public double compareResumeAndJob(String resumeJson, String jobJson) {
        try {
            /* 1) Build a concise user prompt */
            String prompt = """
                Give ONE number from 0 to 100 (higher = better) that reflects
                how well this résumé matches this job description, focusing on
                skills, education, and years of experience.

                Return ONLY the number, nothing else.

                RESUME_JSON:
                %s

                JOB_JSON:
                %s
                """.formatted(resumeJson, jobJson);

            /* 2) Craft the JSON payload for Chat Completions */
            String bodyJson = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "user", "content": %s}
                  ],
                  "temperature": 0.2,
                  "max_tokens": 4
                }
                """.formatted(
                    MODEL,
                    JSON.writeValueAsString(prompt)      // escapes nicely
            );

            /* 3) Send the HTTPS request */
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            String resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString())
                    .body();

            /* 4) Extract the assistant’s reply (should be a number) */
            String content = JSON.readTree(resp)
                    .path("choices").get(0)
                    .path("message").path("content").asText().trim();

            double score = Double.parseDouble(content);
            log.debug("OpenAI returned LLMscore={}", score);
            return score;

        } catch (Exception e) {
            log.error("OpenAI call failed – returning 0. Error: {}", e.getMessage());
            return 0.0;   // graceful fallback
        }
    }
}
