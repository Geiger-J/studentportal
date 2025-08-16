package com.example.studentportal.model;

/**
 * Enum representing the status of tutoring requests.
 * Manages the lifecycle of requests from creation to completion.
 */
public enum RequestStatus {
    PENDING("Pending"),
    MATCHED("Matched"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}