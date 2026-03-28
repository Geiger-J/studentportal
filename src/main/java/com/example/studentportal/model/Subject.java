package com.example.studentportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

// Model: JPA entity for an academic subject
//
// - persist subject identity (code and display name)
// - serve as FK target from User and Request
@Entity
@Table(name = "subjects") // maps to DB table "subjects"
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment PK
    private Long id;

    @Column(unique = true, nullable = false) // unique + not-null [code is the natural key]
    @NotBlank(message = "Subject code is required")
    private String code;

    @Column(nullable = false) // not-null constraint
    @NotBlank(message = "Display name is required")
    private String displayName;

    public Subject() {}

    public Subject(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    // accessors and mutators
    public Long getId() { return id; }

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
        return "Subject{" + "id=" + id + ", code='" + code + '\'' + ", displayName='" + displayName + '\'' + '}';
    }
}