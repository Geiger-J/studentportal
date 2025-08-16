package com.example.studentportal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import com.example.studentportal.model.Timeslot;

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

    /**
     * Computes the earliest LocalDateTime from weekStartDate and Timeslot definitions.
     * 
     * @param weekStartDate the Monday of the week
     * @param timeslots the set of timeslots for the request
     * @return the earliest LocalDateTime for any of the timeslots in the week
     */
    public static LocalDateTime getEarliestSessionTime(LocalDate weekStartDate, Set<Timeslot> timeslots) {
        if (weekStartDate == null || timeslots == null || timeslots.isEmpty()) {
            return null;
        }

        LocalDateTime earliest = null;
        
        for (Timeslot timeslot : timeslots) {
            LocalDateTime sessionTime = getTimeslotDateTime(weekStartDate, timeslot);
            if (earliest == null || (sessionTime != null && sessionTime.isBefore(earliest))) {
                earliest = sessionTime;
            }
        }
        
        return earliest;
    }

    /**
     * Converts a Timeslot to a LocalDateTime for the given week.
     * TODO: Replace with actual period start times when they are defined.
     * 
     * @param weekStartDate the Monday of the week
     * @param timeslot the timeslot
     * @return the LocalDateTime for the timeslot in the given week
     */
    private static LocalDateTime getTimeslotDateTime(LocalDate weekStartDate, Timeslot timeslot) {
        // TODO: Replace this basic mapping with actual school period times
        DayOfWeek dayOfWeek;
        int period;
        
        String name = timeslot.name();
        String[] parts = name.split("_");
        
        // Parse day
        switch (parts[0]) {
            case "MON": dayOfWeek = DayOfWeek.MONDAY; break;
            case "TUE": dayOfWeek = DayOfWeek.TUESDAY; break;
            case "WED": dayOfWeek = DayOfWeek.WEDNESDAY; break;
            case "THU": dayOfWeek = DayOfWeek.THURSDAY; break;
            case "FRI": dayOfWeek = DayOfWeek.FRIDAY; break;
            default: return null;
        }
        
        // Parse period number
        try {
            period = Integer.parseInt(parts[1].substring(1)); // Remove 'P' prefix
        } catch (NumberFormatException e) {
            return null;
        }
        
        // Basic mapping: Period 1 starts at 9:00, each period is 1 hour
        LocalTime periodStartTime = LocalTime.of(8 + period, 0);
        LocalDate sessionDate = weekStartDate.with(dayOfWeek);
        
        return LocalDateTime.of(sessionDate, periodStartTime);
    }
}