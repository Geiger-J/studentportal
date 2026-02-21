package com.example.studentportal.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Utility class providing the canonical timeslot catalog.
 * Defines all allowed slot codes, their display labels, and grouping by day/period.
 * Used to validate incoming slot strings and generate display labels.
 */
@Component("timeslots")
public class Timeslots {

    /** Ordered list of all valid timeslot codes. */
    public static final List<String> ALL_CODES;

    /** Set of all valid timeslot codes for fast lookup. */
    public static final Set<String> ALL_CODES_SET;

    /** Map from slot code to human-readable label. */
    public static final Map<String, String> LABELS;

    private static final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI"};
    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final int PERIODS = 7;
    // end time for each period (index 0 = P1, 1 = P2, …)
    private static final String[] PERIOD_TIMES = {
        "09:00-09:50", "09:55-10:45", "11:05-11:55",
        "12:00-12:50", "14:05-14:55", "15:00-15:50", "16:00-17:15"
    };

    // separator between day code and period number in slot codes (e.g. "MON_P1")
    private static final String TIMESLOT_SEPARATOR = "_P";

    static {
        List<String> codes = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (int d = 0; d < DAYS.length; d++) {
            for (int p = 1; p <= PERIODS; p++) {
                String code = DAYS[d] + "_P" + p;
                String label = DAY_NAMES[d] + ", P" + p;
                codes.add(code);
                labels.put(code, label);
            }
        }

        ALL_CODES = Collections.unmodifiableList(codes);
        ALL_CODES_SET = Collections.unmodifiableSet(new LinkedHashSet<>(codes));
        LABELS = Collections.unmodifiableMap(labels);
    }

    /**
     * Returns the human-readable label for a timeslot code.
     * @param code slot code like "MON_P1"
     * @return label like "Monday Period 1 (09:00-09:50)", or the code itself if unknown
     */
    public String label(String code) {
        return LABELS.getOrDefault(code, code);
    }

    /**
     * Validates whether the given code is an allowed timeslot.
     * @param code slot code to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String code) {
        return code != null && ALL_CODES_SET.contains(code);
    }

    /**
     * Filters a collection of slot strings, keeping only valid codes.
     * @param codes input collection
     * @return new set containing only valid codes
     */
    public Set<String> filterValid(Collection<String> codes) {
        if (codes == null) return new HashSet<>();
        Set<String> valid = new LinkedHashSet<>();
        for (String code : codes) {
            if (isValid(code)) valid.add(code);
        }
        return valid;
    }

    /**
     * Calculates the exact end date-time for a given timeslot within a specific week.
     *
     * - weekStart must be the Monday of the week (use DateUtil.getMondayOfWeek).
     * - code is like "MON_P1", "FRI_P7", etc.
     * - Returns null if the code or weekStart is invalid.
     *
     * Example: weekStart=2025-01-20 (Mon), code="TUE_P2" → 2025-01-21 10:45
     */
    public static LocalDateTime getTimeslotEndTime(LocalDate weekStart, String code) {
        if (weekStart == null || code == null) return null;

        // split "TUE_P3" into ["TUE", "3"] using the separator constant
        String[] parts = code.split(TIMESLOT_SEPARATOR);
        if (parts.length != 2) return null;

        String day = parts[0];
        int period;
        try {
            period = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (period < 1 || period > PERIODS) return null;

        // day offset from Monday (MON=0, TUE=1, …, FRI=4)
        int dayOffset = -1;
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) { dayOffset = i; break; }
        }
        if (dayOffset < 0) return null;

        // PERIOD_TIMES[period-1] is like "09:00-09:50"; take the part after "-"
        String endStr = PERIOD_TIMES[period - 1].split("-")[1]; // "09:50"
        String[] hm = endStr.split(":");
        int hour   = Integer.parseInt(hm[0]);
        int minute = Integer.parseInt(hm[1]);

        return weekStart.plusDays(dayOffset).atTime(hour, minute);
    }
}
