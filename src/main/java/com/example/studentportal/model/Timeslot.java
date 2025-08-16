package com.example.studentportal.model;

/**
 * Enum representing timeslots across the school week.
 * Each day has 7 periods from Monday to Friday.
 * Provides controlled vocabulary for scheduling.
 */
public enum Timeslot {
    // Monday periods
    MON_P1, MON_P2, MON_P3, MON_P4, MON_P5, MON_P6, MON_P7,
    
    // Tuesday periods
    TUE_P1, TUE_P2, TUE_P3, TUE_P4, TUE_P5, TUE_P6, TUE_P7,
    
    // Wednesday periods
    WED_P1, WED_P2, WED_P3, WED_P4, WED_P5, WED_P6, WED_P7,
    
    // Thursday periods
    THU_P1, THU_P2, THU_P3, THU_P4, THU_P5, THU_P6, THU_P7,
    
    // Friday periods
    FRI_P1, FRI_P2, FRI_P3, FRI_P4, FRI_P5, FRI_P6, FRI_P7;

    /**
     * Returns a human-readable label for the timeslot.
     * @return formatted string like "Monday Period 1"
     */
    public String label() {
        String[] parts = name().split("_");
        String day = formatDay(parts[0]);
        String period = parts[1].substring(1); // Remove 'P' prefix
        return day + " Period " + period;
    }

    private String formatDay(String dayCode) {
        switch (dayCode) {
            case "MON": return "Monday";
            case "TUE": return "Tuesday";
            case "WED": return "Wednesday";
            case "THU": return "Thursday";
            case "FRI": return "Friday";
            default: return dayCode;
        }
    }
}