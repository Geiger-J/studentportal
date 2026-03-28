
package com.example.studentportal.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Model: JPA entity for user accounts (students and admins)
//
// - persist user identity, credentials, and role
// - track academic profile (year group, exam board, subjects, availability)
// - determine and cache profile completeness
@Entity
@Table(name = "users") // maps to DB table "users"
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment PK
    private Long id;
    @Column(nullable = false) // not-null constraint
    @NotBlank(message = "Full name is required")
    private String fullName;
    @Column(unique = true, nullable = false) // unique + not-null at DB level
    @Email(message = "Valid email is required")
    @Pattern(regexp = ".*@example\\.edu$", message = "Email must end with @example.edu")
    private String email;
    @Column(nullable = false) // not-null constraint
    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 4 characters")
    private String passwordHash;
    @Column(nullable = false) // not-null constraint
    private String role;
    @Min(value = 9, message = "Year group must be between 9 and 13")
    @Max(value = 13, message = "Year group must be between 9 and 13")
    private Integer yearGroup;
    @Column
    private String examBoard = "NONE"; // default [overridden when yearGroup is set]
    @ManyToMany(fetch = FetchType.EAGER) // eager-load subjects with user
    @JoinTable(name = "user_subjects", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "subject_id")) // join
                                                                                                                                           // table
                                                                                                                                           // for
                                                                                                                                           // user-subject
                                                                                                                                           // M:M
    private Set<Subject> subjects = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) // eager-load availability set
    @CollectionTable(name = "user_availability", joinColumns = @JoinColumn(name = "user_id")) // separate
                                                                                              // table
                                                                                              // for
                                                                                              // availability
                                                                                              // strings
    @Column(name = "timeslot") // column name in collection table
    private Set<String> availability = new HashSet<>();
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER) // requests owned by this user
    private Set<Request> requests = new HashSet<>();
    @Column(nullable = false) // not-null constraint
    private Boolean profileComplete = false; // cached flag [updated via updateProfileCompleteness]
    @CreationTimestamp
    @Column(nullable = false, updatable = false) // immutable after insert
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false) // not-null constraint
    private LocalDateTime updatedAt;

    public User() {}

    public User(String fullName, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // accessors and mutators
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public Integer getYearGroup() { return yearGroup; }

    // set year group and auto-derive exam board default
    public void setYearGroup(Integer yearGroup) {
        this.yearGroup = yearGroup;
        if (yearGroup != null) {
            if (yearGroup >= 9 && yearGroup <= 11) {
                this.examBoard = "GCSE";
            } else if (yearGroup >= 12 && yearGroup <= 13) {
                if ("GCSE".equals(this.examBoard) || "NONE".equals(this.examBoard)) {
                    this.examBoard = "NONE"; // years 12-13: must choose A_LEVELS or IB explicitly
                }
            } else {
                this.examBoard = "NONE";
            }
        }
    }

    public String getExamBoard() { return examBoard; }

    public void setExamBoard(String examBoard) { this.examBoard = examBoard; }

    public Set<Subject> getSubjects() {
        if (subjects == null) {
            subjects = new HashSet<>();
        }
        return subjects;
    }

    public void setSubjects(Set<Subject> subjects) { this.subjects = (subjects != null) ? subjects : new HashSet<>(); }

    public Set<String> getAvailability() {
        if (availability == null) {
            availability = new HashSet<>();
        }
        return availability;
    }

    public void setAvailability(Set<String> availability) {
        this.availability = (availability != null) ? availability : new HashSet<>();
    }

    public Set<Request> getRequests() {
        if (requests == null) {
            requests = new HashSet<>();
        }
        return requests;
    }

    public void setRequests(Set<Request> requests) { this.requests = (requests != null) ? requests : new HashSet<>(); }

    public Boolean getProfileComplete() { return profileComplete; }

    public void setProfileComplete(Boolean profileComplete) { this.profileComplete = profileComplete; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // admin always complete; students need year, subjects, and availability
    public boolean isProfileComplete() {
        // admin users bypass academic profile requirement
        if ("ADMIN".equals(role)) {
            return true;
        }
        return yearGroup != null && yearGroup >= 9 && yearGroup <= 13 && !getSubjects().isEmpty()
                && !getAvailability().isEmpty();
    }

    public void updateProfileCompleteness() { this.profileComplete = isProfileComplete(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return email != null && email.equals(user.email);
    }

    @Override
    public int hashCode() { return email != null ? email.hashCode() : 0; }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", fullName='" + fullName + '\'' + ", email='" + email + '\'' + ", role=" + role
                + ", yearGroup=" + yearGroup + ", examBoard=" + examBoard + ", profileComplete=" + profileComplete
                + '}';
    }
}
