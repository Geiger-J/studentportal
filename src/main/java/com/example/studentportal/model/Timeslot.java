package com.example.studentportal.model;

/**
 * Enum representing timeslots across the school week.
 * Each day has 7 periods from Monday to Friday with specific times in Europe/London timezone.
 * P1 09:00-09:50, P2 09:55-10:45, P3 11:05-11:55, P4 12:00-12:50, 
 * P5 14:05-14:55, P6 15:00-15:50, P7 16:00-17:15
 */
public enum Timeslot {
    // Monday periods
    MON_P1("Monday", 1, "09:00-09:50"), 
    MON_P2("Monday", 2, "09:55-10:45"), 
    MON_P3("Monday", 3, "11:05-11:55"), 
    MON_P4("Monday", 4, "12:00-12:50"), 
    MON_P5("Monday", 5, "14:05-14:55"), 
    MON_P6("Monday", 6, "15:00-15:50"), 
    MON_P7("Monday", 7, "16:00-17:15"),
    
    // Tuesday periods
    TUE_P1("Tuesday", 1, "09:00-09:50"), 
    TUE_P2("Tuesday", 2, "09:55-10:45"), 
    TUE_P3("Tuesday", 3, "11:05-11:55"), 
    TUE_P4("Tuesday", 4, "12:00-12:50"), 
    TUE_P5("Tuesday", 5, "14:05-14:55"), 
    TUE_P6("Tuesday", 6, "15:00-15:50"), 
    TUE_P7("Tuesday", 7, "16:00-17:15"),
    
    // Wednesday periods
    WED_P1("Wednesday", 1, "09:00-09:50"), 
    WED_P2("Wednesday", 2, "09:55-10:45"), 
    WED_P3("Wednesday", 3, "11:05-11:55"), 
    WED_P4("Wednesday", 4, "12:00-12:50"), 
    WED_P5("Wednesday", 5, "14:05-14:55"), 
    WED_P6("Wednesday", 6, "15:00-15:50"), 
    WED_P7("Wednesday", 7, "16:00-17:15"),
    
    // Thursday periods
    THU_P1("Thursday", 1, "09:00-09:50"), 
    THU_P2("Thursday", 2, "09:55-10:45"), 
    THU_P3("Thursday", 3, "11:05-11:55"), 
    THU_P4("Thursday", 4, "12:00-12:50"), 
    THU_P5("Thursday", 5, "14:05-14:55"), 
    THU_P6("Thursday", 6, "15:00-15:50"), 
    THU_P7("Thursday", 7, "16:00-17:15"),
    
    // Friday periods
    FRI_P1("Friday", 1, "09:00-09:50"), 
    FRI_P2("Friday", 2, "09:55-10:45"), 
    FRI_P3("Friday", 3, "11:05-11:55"), 
    FRI_P4("Friday", 4, "12:00-12:50"), 
    FRI_P5("Friday", 5, "14:05-14:55"), 
    FRI_P6("Friday", 6, "15:00-15:50"), 
    FRI_P7("Friday", 7, "16:00-17:15");

    private final String dayName;
    private final int periodNumber;
    private final String timeRange;

    Timeslot(String dayName, int periodNumber, String timeRange) {
        this.dayName = dayName;
        this.periodNumber = periodNumber;
        this.timeRange = timeRange;
    }

    /**
     * Returns a human-readable label for the timeslot.
     * @return formatted string like "Monday Period 1 (09:00-09:50)"
     */
    public String label() {
        return dayName + " Period " + periodNumber + " (" + timeRange + ")";
    }

    /**
     * Returns the day name.
     * @return day name like "Monday"
     */
    public String getDayName() {
        return dayName;
    }

    /**
     * Returns the period number.
     * @return period number (1-7)
     */
    public int getPeriodNumber() {
        return periodNumber;
    }

    /**
     * Returns the time range in Europe/London timezone.
     * @return time range like "09:00-09:50"
     */
    public String getTimeRange() {
        return timeRange;
    }
}