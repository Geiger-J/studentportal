package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.RequestStatus;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
     * Saturday matching and archival process triggered every Saturday at 11:00 PM Europe/London time.
     * First runs matching for current week, then archives previous week requests.
     */
    @Scheduled(cron = "0 0 23 * * SAT", zone = "Europe/London")
    public void performWeeklyMatchingAndArchival() {
        logger.info("Starting Saturday matching and archival process...");
        
        try {
            // Phase 1: Matching for current week
            int matchedCount = performMatching();
            logger.info("Matching phase completed. Processed {} requests.", matchedCount);
            
            // Phase 2: Archival of previous week
            int archivedCount = performArchival();
            logger.info("Archival phase completed. Archived {} requests.", archivedCount);
            
            logger.info("Saturday matching and archival process completed successfully.");
        } catch (Exception e) {
            logger.error("Error during Saturday matching and archival process", e);
        }
    }

    /**
     * Manual matching trigger - processes current week PENDING requests.
     * Attempts to match requests; matched => MATCHED, unmatched => NOT_MATCHED.
     * 
     * @return number of requests processed
     */
    @Transactional
    public int performMatching() {
        LocalDate currentWeekStart = getCurrentWeekStart();
        
        List<Request> pendingRequests = requestRepository
            .findByStatusAndWeekStartDate(RequestStatus.PENDING, currentWeekStart);
        
        int processedCount = 0;
        for (Request request : pendingRequests) {
            // Basic matching logic - for now, just set to NOT_MATCHED
            // TODO: Implement actual matching algorithm
            request.setStatus(RequestStatus.NOT_MATCHED);
            requestRepository.save(request);
            processedCount++;
        }
        
        logger.info("Processed {} pending requests for week starting {}", processedCount, currentWeekStart);
        
        return processedCount;
    }

    /**
     * Manual archival trigger - archives old requests immediately.
     * Archives all requests from PREVIOUS week where status IN (MATCHED, NOT_MATCHED, PENDING safety net).
     * 
     * @return number of requests archived
     */
    @Transactional
    public int performArchival() {
        // Archive requests older than current week
        LocalDate currentWeekStart = getCurrentWeekStart();
        
        List<RequestStatus> statusesToArchive = List.of(
            RequestStatus.PENDING, 
            RequestStatus.MATCHED,
            RequestStatus.NOT_MATCHED,
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
     * Gets the current week start date (Monday).
     * @return LocalDate representing Monday of current week
     */
    private LocalDate getCurrentWeekStart() {
        return DateUtil.getMondayOfWeek(LocalDate.now());
    }
}