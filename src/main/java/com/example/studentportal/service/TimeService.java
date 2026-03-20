package com.example.studentportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Service – wall-clock abstraction supporting simulation override for testing
 *
 * Responsibilities:
 * - return real LocalDateTime.now() in production
 * - return configured fixed timestamp when app.simulation.datetime is set [e.g., application-local.properties]
 */
@Service
public class TimeService {

    // ISO-8601 datetime string, e.g. "2025-01-20T10:00:00"
    // blank/missing = use real time
    @Value("${app.simulation.datetime:}")
    private String simulationDatetime;

    // real time unless simulation override is set
    public LocalDateTime now() {
        if (simulationDatetime != null && !simulationDatetime.isBlank()) {
            return LocalDateTime.parse(simulationDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    // date part of now()
    public LocalDate today() { return now().toLocalDate(); }
}
