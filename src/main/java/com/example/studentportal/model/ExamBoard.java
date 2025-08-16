package com.example.studentportal.model;

/**
 * Enum representing different examination boards available at the school.
 * Assignment is based on year group with some user choice for senior years.
 */
public enum ExamBoard {
    GCSE("GCSE"),
    A_LEVELS("A Levels"), 
    IB("International Baccalaureate"),
    NONE("None");

    private final String displayName;

    ExamBoard(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}