package com.yourname.backend.dto;

import java.util.Set;

/**
 * Read‑only payload returned by /resume/upload and list endpoints.
 * Java 17+ record gives you getters, equals/hashCode, toString for free.
 */
public record ResumeDto(
        Long id,
        String fileName,
        String candidateName,
        double matchScore,
        double semanticScore,
        double skillsScore,
        double educationScore,
        double experienceScore,
        double overlap,
        double llmScore,
        String email,
        String phone,
        String summary,
        String education,
        Set<String> skills,
        String status
) {}