// src/main/java/com/yourname/backend/entities/Experience.java
package com.yourname.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "experiences")
public class Experience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    // new:
    @Column(columnDefinition = "text")
    private String description;

    // (you can keep role/company/years if you later want to split them out,
    //  but for now we'll store the raw parser output here)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
