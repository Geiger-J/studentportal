package com.example.studentportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing tutoring requests within the system.
 * Users can create requests to offer or seek tutoring in specific subjects and timeslots.
 */
@Entity
@Table(name = "requests")
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Request type is required")
    private RequestType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @NotNull(message = "Subject is required")
    private Subject subject;
    
    @ElementCollection(targetClass = Timeslot.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "request_timeslots", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "timeslot")
    private Set<Timeslot> timeslots = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_timeslot")
    private Timeslot chosenTimeslot;
    
    @Column(nullable = true)
    private LocalDate weekStartDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    @Column(nullable = false)
    private Boolean archived = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_partner_id")
    private User matchedPartner;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Request() {}

    public Request(User user, RequestType type, Subject subject, Set<Timeslot> timeslots) {
        this.user = user;
        this.type = type;
        this.subject = subject;
        this.timeslots = timeslots != null ? new HashSet<>(timeslots) : new HashSet<>();
        this.status = RequestStatus.PENDING;
        this.archived = false;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Set<Timeslot> getTimeslots() {
        return timeslots;
    }

    public void setTimeslots(Set<Timeslot> timeslots) {
        this.timeslots = timeslots != null ? timeslots : new HashSet<>();
    }

    public Timeslot getChosenTimeslot() {
        return chosenTimeslot;
    }

    public void setChosenTimeslot(Timeslot chosenTimeslot) {
        this.chosenTimeslot = chosenTimeslot;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived != null ? archived : false;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public User getMatchedPartner() {
        return matchedPartner;
    }

    public void setMatchedPartner(User matchedPartner) {
        this.matchedPartner = matchedPartner;
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

    /**
     * Checks if this request can be cancelled (i.e., is currently PENDING).
     */
    public boolean canBeCancelled() {
        return RequestStatus.PENDING.equals(this.status);
    }

    /**
     * Cancels this request if it's currently pending.
     */
    public void cancel() {
        if (canBeCancelled()) {
            this.status = RequestStatus.CANCELLED;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + id +
                ", type=" + type +
                ", subject=" + (subject != null ? subject.getDisplayName() : "null") +
                ", timeslots=" + timeslots.size() +
                ", chosenTimeslot=" + chosenTimeslot +
                ", weekStartDate=" + weekStartDate +
                ", status=" + status +
                ", archived=" + archived +
                '}';
    }
}