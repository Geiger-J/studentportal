
package com.example.studentportal.model;

/**
 * Enum represents different exam boards available at the school.
 * Exam board is based on year group with some choice for sixth form.
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