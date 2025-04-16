package com.yourname.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String descriptionText;

    public JobDescription() {
    }

    public JobDescription(String title, String descriptionText) {
        this.title = title;
        this.descriptionText = descriptionText;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescriptionText() {
        return descriptionText;
    }
    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }
}
