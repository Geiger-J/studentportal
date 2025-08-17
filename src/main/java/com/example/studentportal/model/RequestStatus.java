package com.example.studentportal.model;

/**
 * Enum representing the status of tutoring requests.
 * Manages the lifecycle of requests from creation to completion.
 * ARCHIVED status removed - now handled by boolean archived field.
 */
public enum RequestStatus {
    PENDING("Pending"),
    MATCHED("Matched"),
    NOT_MATCHED("Not Matched"),
    DONE("Done"),
    CANCELLED("Cancelled");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}