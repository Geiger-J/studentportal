package com.example.studentportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Service: provides current time, with optional simulation override
//
// - return real wall-clock time by default
// - return a fixed simulation timestamp when app.simulation.datetime is set
@Service
public class TimeService {

    // ISO-8601 datetime string [e.g. "2025-01-20T10:00:00"]; blank = use real time
    @Value("${app.simulation.datetime:}") // inject simulation override [blank = real time]
    private String simulationDatetime;

    // real time or configured simulation timestamp
    public LocalDateTime now() {
        if (simulationDatetime != null && !simulationDatetime.isBlank()) {
            return LocalDateTime.parse(simulationDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    // date part of now()
    public LocalDate today() { return now().toLocalDate(); }
}
