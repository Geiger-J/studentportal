package com.example.studentportal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/*
 * Utility – date helpers for week-boundary calculations
 *
 * Responsibilities:
 * - compute next Monday strictly after a given date
 * - get the Monday of the week containing a given date
 */
public class DateUtil {

    // next Monday strictly after date [if today is Monday, returns next week's Monday]
    public static LocalDate nextMonday(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        return date.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate nextMonday() { return nextMonday(LocalDate.now()); }

    public static boolean isMonday(LocalDate date) {
        return date != null && date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    // Monday of the week containing date [or date itself if already Monday]
    public static LocalDate getMondayOfWeek(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}