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

// Service: scheduled job to auto-complete matched requests
//
// Responsibilities:
// - poll MATCHED requests every 60 s and mark as DONE when timeslot has passed
// - skip processing in "test" profile to avoid test interference
// - honour TimeService simulation time during local testing
@Component
@Profile("!test") // skip scheduler in test profile
public class RequestStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RequestStatusScheduler.class);

    private final RequestRepository requestRepository;
    private final TimeService timeService;

    @Autowired
    public RequestStatusScheduler(RequestRepository requestRepository, TimeService timeService) {
        this.requestRepository = requestRepository;
        this.timeService = timeService;
    }

    // poll all MATCHED requests; transition to DONE if timeslot end has passed
    @Scheduled(fixedDelay = 60_000) // every 60 s [60_000 ms]
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
                logger.debug("Marked request {} as DONE (slot {} on week {})", request.getId(),
                        request.getChosenTimeslot(), request.getWeekStartDate());
            }
        }

        if (doneCount > 0) {
            logger.info("Marked {} matched request(s) as DONE", doneCount);
        }
    }

    // true if the request's timeslot end time is in the past; missing data -> skip
    boolean shouldBeMarkedDone(Request request, LocalDateTime now) {
        if (request.getWeekStartDate() == null || request.getChosenTimeslot() == null) {
            return false;
        }
        LocalDateTime endTime = Timeslots.getTimeslotEndTime(request.getWeekStartDate(),
                request.getChosenTimeslot());
        return endTime != null && now.isAfter(endTime);
    }
}
