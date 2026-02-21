package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.util.Timeslots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that automatically marks matched requests as DONE
 * once the timeslot they are scheduled for has passed.
 *
 * - Runs every 60 seconds (configurable via fixedDelay).
 * - Skipped entirely in the "test" Spring profile so unit tests are unaffected.
 * - Uses TimeService so simulation time is respected during local testing.
 *
 * Example: a request with chosenTimeslot="MON_P1" and weekStartDate=2025-01-20
 *   becomes DONE after 09:50 on that Monday.
 */
@Component
@Profile("!test")
public class RequestStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RequestStatusScheduler.class);

    private final RequestRepository requestRepository;
    private final TimeService timeService;

    @Autowired
    public RequestStatusScheduler(RequestRepository requestRepository, TimeService timeService) {
        this.requestRepository = requestRepository;
        this.timeService = timeService;
    }

    /**
     * Checks all MATCHED requests every minute and transitions any whose
     * scheduled timeslot end-time has passed to DONE.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markCompletedRequestsDone() {
        LocalDateTime now = timeService.now();
        List<Request> matched = requestRepository.findByStatus("MATCHED");

        int doneCount = 0;
        for (Request request : matched) {
            if (shouldBeMarkedDone(request, now)) {
                request.setStatus("DONE");
                requestRepository.save(request);
                doneCount++;
                logger.debug("Marked request {} as DONE (slot {} on week {})",
                        request.getId(), request.getChosenTimeslot(), request.getWeekStartDate());
            }
        }

        if (doneCount > 0) {
            logger.info("Marked {} matched request(s) as DONE", doneCount);
        }
    }

    /**
     * Returns true when the request's timeslot end-time is in the past.
     * Requests without a weekStartDate or chosenTimeslot are skipped (they pre-date this feature).
     */
    boolean shouldBeMarkedDone(Request request, LocalDateTime now) {
        if (request.getWeekStartDate() == null || request.getChosenTimeslot() == null) {
            return false;
        }
        LocalDateTime endTime = Timeslots.getTimeslotEndTime(
                request.getWeekStartDate(), request.getChosenTimeslot());
        return endTime != null && now.isAfter(endTime);
    }
}
