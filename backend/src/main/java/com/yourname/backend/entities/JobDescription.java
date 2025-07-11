package com.yourname.backend.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String category;
    private String location;

    @Column(columnDefinition = "text")
    private String descriptionText;

    @Column(nullable = true)
    private String filePath;

    @Column(length = 2000)
    private String summary;

    /* ------------------------------  Skills  ------------------------------ */
    @ManyToMany(cascade = {PERSIST, MERGE})
    @JoinTable(name = "job_skills",
            joinColumns        = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    /* ----------------------  Requirements & Responsibilities -------------- */
    @Column(columnDefinition = "text")
    private String requirements;

    @Column(columnDefinition = "text")
    private String responsibilities;

    /* (optional) raw JSON that came back from your OpenAI parser */
    @Column(columnDefinition = "jsonb")
    private String parsedJson;

    @Column(nullable = false)
    private String status;

    /* --------------------------------------------------------------------- */
    public JobDescription() {
        this.status = "Active";
    }

    /* ---------- getters / setters ---------------------------------------- */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String descriptionText) { this.descriptionText = descriptionText; }

    public Set<Skill> getSkills() { return skills; }
    public void setSkills(Set<Skill> skills) { this.skills = skills; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }

    public String getParsedJson() { return parsedJson; }
    public void setParsedJson(String parsedJson) { this.parsedJson = parsedJson; }

    public String getSummary() { return summary; }
    public void   setSummary(String summary) { this.summary = summary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
