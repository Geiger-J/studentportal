package com.example.studentportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides the "current" time to the rest of the application.
 *
 * - In normal operation it returns the real wall-clock time.
 * - When app.simulation.datetime is set (e.g. in application-local.properties),
 *   it returns that fixed timestamp instead.
 *   This lets you fast-forward time for manual testing without touching production code.
 */
@Service
public class TimeService {

    // ISO-8601 datetime string, e.g. "2025-01-20T10:00:00"
    // blank/missing = use real time
    @Value("${app.simulation.datetime:}")
    private String simulationDatetime;

    /**
     * Returns "now" — either the real time or the configured simulation timestamp.
     */
    public LocalDateTime now() {
        if (simulationDatetime != null && !simulationDatetime.isBlank()) {
            return LocalDateTime.parse(simulationDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    /**
     * Convenience: returns just the date part of now().
     */
    public LocalDate today() {
        return now().toLocalDate();
    }
}
