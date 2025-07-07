package com.yourname.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "skills")
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "skills")
    @JsonIgnore
    private Set<Resume> resumes = new HashSet<>();

    public Skill() {}
    public Skill(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Resume> getResumes() { return resumes; }
    public void setResumes(Set<Resume> resumes) { this.resumes = resumes; }
}
