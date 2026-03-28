package com.example.studentportal.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

// Utility: canonical timeslot catalog and helpers
//
// - define all valid slot codes (MON_P1 .. FRI_P7)
// - map codes to human-readable labels
// - calculate slot end times for the scheduler
@Component("timeslots")
public class Timeslots {

    /** Ordered list of all valid timeslot codes. */
    public static final List<String> ALL_CODES;

    /** Set of all valid timeslot codes for fast lookup. */
    public static final Set<String> ALL_CODES_SET;

    /** Map from slot code to human-readable label. */
    public static final Map<String, String> LABELS;

    private static final String[] DAYS = { "MON", "TUE", "WED", "THU", "FRI" };
    private static final String[] DAY_NAMES = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };
    private static final int PERIODS = 7;
    // end time for each period index [0 = P1, 6 = P7]
    private static final String[] PERIOD_TIMES = { "09:00-09:50", "09:55-10:45", "11:05-11:55", "12:00-12:50",
            "14:05-14:55", "15:00-15:50", "16:00-17:15" };

    // separator between day code and period number [e.g. "MON_P1"]
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

    // label returns display text [e.g. "Monday, P1"]; falls back to code if unknown
    public String label(String code) { return LABELS.getOrDefault(code, code); }

    public boolean isValid(String code) { return code != null && ALL_CODES_SET.contains(code); }

    // filter to only valid codes; null input returns empty set
    public Set<String> filterValid(Collection<String> codes) {
        if (codes == null)
            return new HashSet<>();
        Set<String> valid = new LinkedHashSet<>();
        for (String code : codes) {
            if (isValid(code))
                valid.add(code);
        }
        return valid;
    }

    // compute exact end datetime for a slot within a week; null if code/weekStart
    // invalid
    // weekStart must be the Monday of the week [use DateUtil.getMondayOfWeek]
    // example: weekStart=2025-01-20, code="TUE_P2" -> 2025-01-21T10:45
    public static LocalDateTime getTimeslotEndTime(LocalDate weekStart, String code) {
        if (weekStart == null || code == null)
            return null;

        // split "TUE_P3" -> ["TUE", "3"]
        String[] parts = code.split(TIMESLOT_SEPARATOR);
        if (parts.length != 2)
            return null;

        String day = parts[0];
        int period;
        try {
            period = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (period < 1 || period > PERIODS)
            return null;

        // day offset from Monday [MON=0, TUE=1, ..., FRI=4]
        int dayOffset = -1;
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) {
                dayOffset = i;
                break;
            }
        }
        if (dayOffset < 0)
            return null;

        // PERIOD_TIMES[period-1] like "09:00-09:50"; take part after "-"
        String endStr = PERIOD_TIMES[period - 1].split("-")[1];
        String[] hm = endStr.split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = Integer.parseInt(hm[1]);

        return weekStart.plusDays(dayOffset).atTime(hour, minute);
    }
}
