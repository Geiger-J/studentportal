package com.example.studentportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/*
 * Model – academic subject entity [code + display name]
 *
 * Responsibilities:
 * - uniquely identified by code [e.g., MATHEMATICS]
 * - used to categorise tutoring requests and user profiles
 */
@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Subject code is required")
    private String code;

    @Column(nullable = false)
    @NotBlank(message = "Display name is required")
    private String displayName;

    public Subject() {}

    public Subject(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    // --- accessors
    public Long getId() { return id; }

    // --- mutators
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Subject subject = (Subject) o;
        return code != null && code.equals(subject.code);
    }

    @Override
    public int hashCode() { return code != null ? code.hashCode() : 0; }

    @Override
    public String toString() {
        return "Subject{" + "id=" + id + ", code='" + code + '\'' + ", displayName='" + displayName
                + '\'' + '}';
    }
}