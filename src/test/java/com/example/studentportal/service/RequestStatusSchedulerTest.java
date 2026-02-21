package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.util.Timeslots;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RequestStatusScheduler.shouldBeMarkedDone().
 *
 * We test the logic method directly (no Spring context needed here).
 * The actual @Scheduled execution is disabled in the test profile via @Profile("!test").
 */
class RequestStatusSchedulerTest {

    // minimal stub — only needs status, chosenTimeslot, weekStartDate
    private Request makeMatchedRequest(String timeslot, LocalDate weekStart) {
        Request r = new Request();
        r.setStatus("MATCHED");
        r.setChosenTimeslot(timeslot);
        r.setWeekStartDate(weekStart);
        return r;
    }

    // We test the scheduler logic by calling shouldBeMarkedDone() directly.
    // shouldBeMarkedDone() only reads the Request fields — it never touches the
    // repository or timeService dependencies, so null is safe here for unit-testing.
    private RequestStatusScheduler scheduler() {
        return new RequestStatusScheduler(null, null);
    }

    @Test
    void testShouldBeDone_WhenPastEndTime() {
        // MON_P1 ends at 09:50 on the given Monday
        LocalDate monday = LocalDate.of(2025, 1, 20);
        Request req = makeMatchedRequest("MON_P1", monday);

        // "now" is 09:51 — just past the end of P1
        LocalDateTime now = monday.atTime(9, 51);
        assertTrue(scheduler().shouldBeMarkedDone(req, now));
    }

    @Test
    void testShouldNotBeDone_WhenBeforeEndTime() {
        LocalDate monday = LocalDate.of(2025, 1, 20);
        Request req = makeMatchedRequest("MON_P1", monday);

        // "now" is 09:49 — still during P1
        LocalDateTime now = monday.atTime(9, 49);
        assertFalse(scheduler().shouldBeMarkedDone(req, now));
    }

    @Test
    void testShouldNotBeDone_WhenExactlyAtEndTime() {
        // isAfter is strict — exactly at end time means NOT done yet
        LocalDate monday = LocalDate.of(2025, 1, 20);
        Request req = makeMatchedRequest("MON_P1", monday);

        LocalDateTime endTime = monday.atTime(9, 50);
        assertFalse(scheduler().shouldBeMarkedDone(req, endTime));
    }

    @Test
    void testShouldNotBeDone_WhenNoWeekStartDate() {
        Request req = makeMatchedRequest("MON_P1", null);
        assertFalse(scheduler().shouldBeMarkedDone(req, LocalDateTime.now()));
    }

    @Test
    void testShouldNotBeDone_WhenNoChosenTimeslot() {
        LocalDate monday = LocalDate.of(2025, 1, 20);
        Request req = makeMatchedRequest(null, monday);
        assertFalse(scheduler().shouldBeMarkedDone(req, LocalDateTime.now()));
    }

    @Test
    void testShouldBeDone_FRI_P7_PastEndTime() {
        // FRI_P7 ends at 17:15 — friday is weekStart + 4 days
        LocalDate monday = LocalDate.of(2025, 1, 20);
        Request req = makeMatchedRequest("FRI_P7", monday);

        LocalDate friday = monday.plusDays(4);
        LocalDateTime now = friday.atTime(17, 16); // 1 min past end
        assertTrue(scheduler().shouldBeMarkedDone(req, now));
    }

    @Test
    void testGetTimeslotEndTime_KnownSlots() {
        LocalDate monday = LocalDate.of(2025, 1, 20); // known Monday

        // P1 ends at 09:50 same day
        assertEquals(monday.atTime(9, 50),
                Timeslots.getTimeslotEndTime(monday, "MON_P1"));

        // TUE_P2 ends at 10:45 on Tuesday (monday + 1)
        assertEquals(monday.plusDays(1).atTime(10, 45),
                Timeslots.getTimeslotEndTime(monday, "TUE_P2"));

        // FRI_P7 ends at 17:15 on Friday (monday + 4)
        assertEquals(monday.plusDays(4).atTime(17, 15),
                Timeslots.getTimeslotEndTime(monday, "FRI_P7"));
    }

    @Test
    void testGetTimeslotEndTime_InvalidCode_ReturnsNull() {
        LocalDate monday = LocalDate.of(2025, 1, 20);
        assertNull(Timeslots.getTimeslotEndTime(monday, "INVALID"));
        assertNull(Timeslots.getTimeslotEndTime(null, "MON_P1"));
        assertNull(Timeslots.getTimeslotEndTime(monday, null));
    }
}
