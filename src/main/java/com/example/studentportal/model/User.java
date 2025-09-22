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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Entity representing users in the student portal system. Supports both
 * students and administrators with role-based features.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Column(unique = true, nullable = false)
    @Email(message = "Valid email is required")
    @Pattern(regexp = ".*@bromsgrove-school\\.co\\.uk$", message = "Email must end with @bromsgrove-school.co.uk")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 4 characters")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Min(value = 9, message = "Year group must be between 9 and 13")
    @Max(value = 13, message = "Year group must be between 9 and 13")
    private Integer yearGroup;

    @Enumerated(EnumType.STRING)
    private ExamBoard examBoard = ExamBoard.NONE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_subjects",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> subjects = new HashSet<>();

    @ElementCollection(targetClass = Timeslot.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_availability", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "timeslot")
    private Set<Timeslot> availability = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private Set<Request> requests = new HashSet<>();

    @Min(value = 0, message = "Max tutoring per week must be non-negative")
    private Integer maxTutoringPerWeek = 0;

    @Column(nullable = false)
    private Boolean profileComplete = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getYearGroup() {
        return yearGroup;
    }

    public void setYearGroup(Integer yearGroup) {
        this.yearGroup = yearGroup;
        if (yearGroup != null) {
            if (yearGroup >= 9 && yearGroup <= 11) {
                this.examBoard = ExamBoard.GCSE;
            } else if (yearGroup >= 12 && yearGroup <= 13) {
                if (this.examBoard == ExamBoard.GCSE || this.examBoard == ExamBoard.NONE) {
                    this.examBoard = ExamBoard.NONE; // must choose A_LEVELS or IB
                }
            } else {
                this.examBoard = ExamBoard.NONE;
            }
        }
    }

    public ExamBoard getExamBoard() {
        return examBoard;
    }

    public void setExamBoard(ExamBoard examBoard) {
        this.examBoard = examBoard;
    }

    public Set<Subject> getSubjects() {
        if (subjects == null) {
            subjects = new HashSet<>();
        }
        return subjects;
    }

    public void setSubjects(Set<Subject> subjects) {
        this.subjects = (subjects != null) ? subjects : new HashSet<>();
    }

    public Set<Timeslot> getAvailability() {
        if (availability == null) {
            availability = new HashSet<>();
        }
        return availability;
    }

    public void setAvailability(Set<Timeslot> availability) {
        this.availability = (availability != null) ? availability : new HashSet<>();
    }

    public Set<Request> getRequests() {
        if (requests == null) {
            requests = new HashSet<>();
        }
        return requests;
    }

    public void setRequests(Set<Request> requests) {
        this.requests = (requests != null) ? requests : new HashSet<>();
    }

    public Integer getMaxTutoringPerWeek() {
        return maxTutoringPerWeek;
    }

    public void setMaxTutoringPerWeek(Integer maxTutoringPerWeek) {
        this.maxTutoringPerWeek = maxTutoringPerWeek;
    }

    public Boolean getProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(Boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isProfileComplete() {
        // Admin users don't need academic profile completion
        if (role == Role.ADMIN) {
            return true;
        }
        
        return yearGroup != null
                && yearGroup >= 9 && yearGroup <= 13
                && !getSubjects().isEmpty()
                && !getAvailability().isEmpty();
    }

    public void updateProfileCompleteness() {
        this.profileComplete = isProfileComplete();
    }

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
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", fullName='" + fullName + '\''
                + ", email='" + email + '\''
                + ", role=" + role
                + ", yearGroup=" + yearGroup
                + ", examBoard=" + examBoard
                + ", profileComplete=" + profileComplete
                + '}';
    }
}
