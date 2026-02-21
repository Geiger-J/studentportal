package com.example.studentportal.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TimeService — verifies real-time and simulation-time behaviour.
 */
@SpringBootTest
@ActiveProfiles("test")
class TimeServiceTest {

    @Autowired
    private TimeService timeService;

    @Test
    void testRealTime_NowIsCloseToSystemTime() {
        // with no simulation.datetime set, now() should be very close to real now
        LocalDateTime before = LocalDateTime.now().minusSeconds(5);
        LocalDateTime result = timeService.now();
        LocalDateTime after  = LocalDateTime.now().plusSeconds(5);

        assertTrue(result.isAfter(before) && result.isBefore(after),
                "Real-time now() should be close to LocalDateTime.now()");
    }

    @Test
    void testRealTime_TodayMatchesDate() {
        // today() must equal the date part of now()
        LocalDate expected = timeService.now().toLocalDate();
        assertEquals(expected, timeService.today());
    }

    /**
     * Tests that a simulation datetime can be parsed from a property.
     * We do this by constructing a TimeService directly with the field set.
     */
    @Test
    void testSimulationTime_ParsedCorrectly() throws Exception {
        TimeService sim = new TimeService();
        // inject via field reflection (no setter needed — matches @Value behaviour)
        var field = TimeService.class.getDeclaredField("simulationDatetime");
        field.setAccessible(true);
        field.set(sim, "2025-01-20T09:51:00");

        LocalDateTime result = sim.now();
        assertEquals(2025, result.getYear());
        assertEquals(1,    result.getMonthValue());
        assertEquals(20,   result.getDayOfMonth());
        assertEquals(9,    result.getHour());
        assertEquals(51,   result.getMinute());
    }

    @Test
    void testSimulationTime_TodayMatchesSimDate() throws Exception {
        TimeService sim = new TimeService();
        var field = TimeService.class.getDeclaredField("simulationDatetime");
        field.setAccessible(true);
        field.set(sim, "2025-03-15T14:30:00");

        assertEquals(LocalDate.of(2025, 3, 15), sim.today());
    }
}
