package com.example.studentportal.service;

import com.example.studentportal.model.*;
import com.example.studentportal.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Service for handling matching algorithm and scheduled operations.
 * Includes weekly matching process and Saturday archival in Europe/London timezone.
 */
@Service
@Transactional
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);

    private final RequestRepository requestRepository;

    @Autowired
    public MatchingService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Weekly matching algorithm triggered every Monday at 6:00 AM Europe/London time.
     * Matches TUTOR and TUTEE requests based on subject and overlapping timeslots.
     */
    @Scheduled(cron = "0 0 6 * * MON", zone = "Europe/London")
    public void performWeeklyMatching() {
        logger.info("Starting weekly matching process...");
        
        try {
            int matchedCount = performMatching();
            logger.info("Weekly matching completed. Matched {} requests.", matchedCount);
        } catch (Exception e) {
            logger.error("Error during weekly matching process", e);
        }
    }

    /**
     * Saturday archival process triggered every Saturday at 11:59 PM Europe/London time.
     * Archives old pending and completed requests from previous weeks.
     */
    @Scheduled(cron = "0 59 23 * * SAT", zone = "Europe/London")
    public void performWeeklyArchival() {
        logger.info("Starting weekly archival process...");
        
        try {
            int archivedCount = performArchival();
            logger.info("Weekly archival completed. Archived {} requests.", archivedCount);
        } catch (Exception e) {
            logger.error("Error during weekly archival process", e);
        }
    }

    /**
     * Manual matching trigger - performs the matching algorithm immediately.
     * 
     * @return number of requests matched
     */
    @Transactional
    public int performMatching() {
        List<Request> tutorRequests = requestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .filter(r -> r.getType() == RequestType.TUTOR)
                .toList();

        List<Request> tuteeRequests = requestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .filter(r -> r.getType() == RequestType.TUTEE)
                .toList();

        int matchedCount = 0;

        for (Request tutorRequest : tutorRequests) {
            for (Request tuteeRequest : tuteeRequests) {
                if (canMatch(tutorRequest, tuteeRequest)) {
                    // Create the match
                    tutorRequest.setStatus(RequestStatus.MATCHED);
                    tutorRequest.setMatchedPartner(tuteeRequest.getUser());
                    
                    tuteeRequest.setStatus(RequestStatus.MATCHED);
                    tuteeRequest.setMatchedPartner(tutorRequest.getUser());
                    
                    requestRepository.save(tutorRequest);
                    requestRepository.save(tuteeRequest);
                    
                    matchedCount += 2; // Count both requests as matched
                    
                    logger.info("Matched tutor {} with tutee {} for subject {}", 
                        tutorRequest.getUser().getFullName(),
                        tuteeRequest.getUser().getFullName(),
                        tutorRequest.getSubject().getDisplayName());
                    
                    break; // Move to next tutor request
                }
            }
        }

        return matchedCount;
    }

    /**
     * Manual archival trigger - archives old requests immediately.
     * 
     * @return number of requests archived
     */
    @Transactional
    public int performArchival() {
        // Archive requests older than current week
        LocalDate currentWeekStart = getCurrentWeekStart();
        
        List<RequestStatus> statusesToArchive = List.of(
            RequestStatus.PENDING, 
            RequestStatus.COMPLETED,
            RequestStatus.CANCELLED
        );
        
        List<Request> requestsToArchive = requestRepository
            .findByStatusInAndWeekStartDateBefore(statusesToArchive, currentWeekStart);
        
        int archivedCount = 0;
        for (Request request : requestsToArchive) {
            request.setStatus(RequestStatus.ARCHIVED);
            requestRepository.save(request);
            archivedCount++;
        }
        
        logger.info("Archived {} old requests before week starting {}", archivedCount, currentWeekStart);
        
        return archivedCount;
    }

    /**
     * Checks if two requests can be matched.
     * 
     * @param tutorRequest the tutor request
     * @param tuteeRequest the tutee request
     * @return true if they can be matched
     */
    private boolean canMatch(Request tutorRequest, Request tuteeRequest) {
        // Same subject
        if (!tutorRequest.getSubject().equals(tuteeRequest.getSubject())) {
            return false;
        }

        // Same week
        if (!tutorRequest.getWeekStartDate().equals(tuteeRequest.getWeekStartDate())) {
            return false;
        }

        // Different users
        if (tutorRequest.getUser().equals(tuteeRequest.getUser())) {
            return false;
        }

        // Overlapping timeslots
        Set<Timeslot> tutorSlots = tutorRequest.getTimeslots();
        Set<Timeslot> tuteeSlots = tuteeRequest.getTimeslots();
        
        return tutorSlots.stream().anyMatch(tuteeSlots::contains);
    }

    /**
     * Gets the start of the current week (Monday).
     * 
     * @return LocalDate representing Monday of current week
     */
    private LocalDate getCurrentWeekStart() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // Monday = 1, Sunday = 7
        return today.minusDays(dayOfWeek - 1);
    }
}