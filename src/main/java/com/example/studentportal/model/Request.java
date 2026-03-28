package com.example.studentportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Model: JPA entity for a tutoring request
//
// - persist request type (TUTOR/TUTEE), subject, and candidate timeslots
// - track matching state (status, matched partner, chosen timeslot)
// - provide lifecycle helpers (canBeCancelled, cancel)
@Entity
@Table(name = "requests") // maps to DB table "requests"
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment PK
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) // lazy-load owning user
    @JoinColumn(name = "user_id", nullable = false) // FK to users table [not null]
    @NotNull(message = "User is required")
    private User user;
    @Column(nullable = false) // not-null constraint
    @NotNull(message = "Request type is required")
    private String type;
    @ManyToOne(fetch = FetchType.LAZY) // lazy-load subject
    @JoinColumn(name = "subject_id", nullable = false) // FK to subjects [not null]
    @NotNull(message = "Subject is required")
    private Subject subject;
    @ElementCollection // collection of plain strings
    // separate table for timeslot strings
    @CollectionTable(name = "request_timeslots", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "timeslot") // column name in collection table
    private Set<String> timeslots = new HashSet<>();
    @Column(name = "chosen_timeslot") // nullable; set after matching
    private String chosenTimeslot;
    // Monday of the week this session belongs to - set when the match is made
    @Column(name = "week_start_date") // nullable; set when match is made
    private LocalDate weekStartDate;
    @Column(nullable = false) // not-null constraint
    private String status = "PENDING";
    @Column(nullable = false) // not-null constraint
    private Boolean archived = false;
    @ManyToOne(fetch = FetchType.LAZY) // lazy-load matched partner [null until matched]
    @JoinColumn(name = "matched_partner_id") // nullable FK to matched user
    private User matchedPartner;
    @CreationTimestamp
    @Column(nullable = false, updatable = false) // immutable after insert
    private LocalDateTime createdAt;

    public Request() {}

    public Request(User user, String type, Subject subject, Set<String> timeslots) {
        this.user = user;
        this.type = type;
        this.subject = subject;
        this.timeslots = timeslots != null ? new HashSet<>(timeslots) : new HashSet<>();
        this.status = "PENDING";
        this.archived = false;
    }

    // accessors and mutators
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public Subject getSubject() { return subject; }

    public void setSubject(Subject subject) { this.subject = subject; }

    public Set<String> getTimeslots() { return timeslots; }

    public void setTimeslots(Set<String> timeslots) {
        this.timeslots = timeslots != null ? timeslots : new HashSet<>();
    }

    public String getChosenTimeslot() { return chosenTimeslot; }

    public void setChosenTimeslot(String chosenTimeslot) { this.chosenTimeslot = chosenTimeslot; }

    public LocalDate getWeekStartDate() { return weekStartDate; }

    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public Boolean getArchived() { return archived; }

    public void setArchived(Boolean archived) { this.archived = archived != null ? archived : false; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public User getMatchedPartner() { return matchedPartner; }

    public void setMatchedPartner(User matchedPartner) { this.matchedPartner = matchedPartner; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // true if status allows cancellation
    public boolean canBeCancelled() { return "PENDING".equals(this.status) || "MATCHED".equals(this.status); }

    // transition to CANCELLED if allowed
    public void cancel() {
        if (canBeCancelled()) {
            this.status = "CANCELLED";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Request request = (Request) o;
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }

    @Override
    public String toString() {
        return "Request{" + "id=" + id + ", type=" + type + ", subject="
                + (subject != null ? subject.getDisplayName() : "null") + ", timeslots=" + timeslots.size()
                + ", chosenTimeslot=" + chosenTimeslot + ", status=" + status + ", archived=" + archived + '}';
    }
}
