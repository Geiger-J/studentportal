package com.example.studentportal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

// Utility: date and week calculation helpers
//
// - compute the Monday strictly after a given date
// - determine the Monday of the week containing a given date
public class DateUtil {

    // next Monday strictly after date; if today is Monday, returns NEXT Monday
    public static LocalDate nextMonday(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        // plusDays(1) ensures we skip today even if today is Monday
        return date.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    // convenience: next Monday from today
    public static LocalDate nextMonday() { return nextMonday(LocalDate.now()); }

    public static boolean isMonday(LocalDate date) {
        return date != null && date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    // Monday of the week containing date [used for weekStartDate on matched
    // requests]
    public static LocalDate getMondayOfWeek(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}