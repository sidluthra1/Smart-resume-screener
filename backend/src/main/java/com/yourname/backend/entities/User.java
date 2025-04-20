package com.yourname.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // Specifies the table name in the database
public class User {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configures auto-increment for the ID
    private Long id;

    @Column(nullable = false, unique = true) // Email must be provided and unique
    private String email;

    @Column(nullable = false) // Password must be provided
    private String password; // Note: This should store a hashed password, not plain text

    @Column(nullable = false) // Role must be provided
    private String role; // e.g., "HR", "ADMIN", "CANDIDATE"

    // Default constructor required by JPA
    public User() {
    }

    // Constructor for creating new users
    public User(String email, String password, String role) {
        this.email = email;
        this.password = password; // Remember to hash this before saving
        this.role = role;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        // Usually, you'd hash the password here or in the service layer
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}