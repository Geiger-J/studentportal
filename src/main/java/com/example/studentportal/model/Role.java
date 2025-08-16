package com.example.studentportal.model;

/**
 * Enum representing user roles within the system.
 * Determines access levels and UI features available.
 */
public enum Role {
    STUDENT("Student"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}