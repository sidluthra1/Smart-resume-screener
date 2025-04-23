package com.yourname.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;


@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String candidateName;
    private LocalDateTime uploadDate;

    private String filePath; // Path where the file is stored

    @Column(nullable = false)
    private String contentType; // e.g., "application/pdf"

    @Column(nullable = false)
    private long size; // file size in bytes

    @Column(nullable = true) // Score might not be calculated immediately
    private Double matchScore; // Score from 0.0 to 100.0

    @Column(length = 320)                 private String email;
    @Column(length = 32)                  private String phone;
    @Column(columnDefinition = "text")    private String summary;
    @Column(length = 256)                  private String education;

    @ManyToMany(cascade = {PERSIST, MERGE})
    @JoinTable(name = "resume_skills",
            joinColumns  = @JoinColumn(name="resume_id"),
            inverseJoinColumns = @JoinColumn(name="skill_id"))
    private Set<Skill> skills = new HashSet<>();

    @OneToMany(mappedBy = "resume",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Experience> experiences = new ArrayList<>();



    // Default constructor required by JPA
    public Resume() {
    }

    public Resume(String fileName, String candidateName, String filePath) {
        this.fileName = fileName;
        this.candidateName = candidateName;
        this.filePath = filePath;
        this.uploadDate = LocalDateTime.now(); // Set upload time automatically
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public Set<Skill> getSkills() { return skills; }

    public void setSkills(Set<Skill> skills) { this.skills = skills; }

    public List<Experience> getExperiences() { return experiences; }
    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences;
    }
    public String getEducation() {
        return education;
    }
    public void setEducation(String education) {
        this.education = education;
    }
}
