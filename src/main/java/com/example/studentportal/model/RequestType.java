package com.example.studentportal.model;

/**
 * Enum representing the type of tutoring request.
 * Users can either offer tutoring (TUTOR) or seek tutoring (TUTEE).
 */
public enum RequestType {
    TUTOR("Offering Tutoring"),
    TUTEE("Seeking Tutoring");

    private final String displayName;

    RequestType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}