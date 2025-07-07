package com.yourname.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "experiences")
public class Experience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(columnDefinition = "text")
    private String description;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
