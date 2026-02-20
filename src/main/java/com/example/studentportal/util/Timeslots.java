package com.example.studentportal.util;

import org.springframework.stereotype.Component;

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
    private static final String[] PERIOD_TIMES = {
        "09:00-09:50", "09:55-10:45", "11:05-11:55",
        "12:00-12:50", "14:05-14:55", "15:00-15:50", "16:00-17:15"
    };

    static {
        List<String> codes = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (int d = 0; d < DAYS.length; d++) {
            for (int p = 1; p <= PERIODS; p++) {
                String code = DAYS[d] + "_P" + p;
                String label = DAY_NAMES[d] + " Period " + p + " (" + PERIOD_TIMES[p - 1] + ")";
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
}
