// src/main/java/com/yourname/backend/dto/JobDescriptionDto.java
package com.yourname.backend.dto;

import java.util.List;

public record JobDescriptionDto(
        Long   id,
        String title,
        String category,
        String location,
        String summary,
        List<String> skills,
        List<String> requirements,
        List<String> responsibilities
) {}
