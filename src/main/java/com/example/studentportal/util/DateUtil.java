package com.example.studentportal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Utility class for date and week calculations.
 * Handles week start date logic for scheduling functionality.
 */
public class DateUtil {

    /**
     * Calculates the next Monday strictly after the given date.
     * This ensures we always schedule for future weeks, never the current week.
     * 
     * Reasoning: Even if today is Monday, we choose the next Monday to allow
     * sufficient time for scheduling runs and matching processes.
     * 
     * @param date the reference date
     * @return the LocalDate of the next Monday after the given date
     */
    public static LocalDate nextMonday(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        // Always get the Monday that comes after the given date
        // If today is Monday, this will return next Monday
        return date.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Gets the next Monday from today.
     * Convenience method for the most common use case.
     * 
     * @return the LocalDate of the next Monday after today
     */
    public static LocalDate nextMonday() {
        return nextMonday(LocalDate.now());
    }

    /**
     * Checks if the given date is a Monday.
     * 
     * @param date the date to check
     * @return true if the date is a Monday, false otherwise
     */
    public static boolean isMonday(LocalDate date) {
        return date != null && date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    /**
     * Gets the Monday of the week containing the given date.
     * 
     * @param date the reference date
     * @return the Monday of the week containing the given date
     */
    public static LocalDate getMondayOfWeek(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}