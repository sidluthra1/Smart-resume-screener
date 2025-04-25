// src/main/java/com/yourname/backend/dto/JobRequest.java
package com.yourname.backend.dto;

public class JobRequest {
    private String title;
    private String descriptionText;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String descriptionText) { this.descriptionText = descriptionText; }
}