package com.example.studentportal.service;

import com.example.studentportal.model.RequestStatus;
import com.example.studentportal.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for providing admin dashboard summary statistics.
 * Provides counts for different request statuses and timeframes.
 */
@Service
@Transactional(readOnly = true)
public class AdminSummaryService {

    private final RequestRepository requestRepository;

    @Autowired
    public AdminSummaryService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Gets summary statistics for the admin dashboard.
     * 
     * @return AdminSummary containing counts
     */
    public AdminSummary getSummary() {
        LocalDate currentWeekStart = getCurrentWeekStart();
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Get all requests for current week
        List<com.example.studentportal.model.Request> currentWeekRequests = requestRepository
            .findByWeekStartDateGreaterThanEqualAndWeekStartDateLessThan(currentWeekStart, nextWeekStart);

        long pendingCount = currentWeekRequests.stream()
            .filter(r -> r.getStatus() == RequestStatus.PENDING)
            .count();

        // Since NOT_MATCHED doesn't exist, we'll use pending requests as "not matched yet"
        long notMatchedCount = pendingCount;

        long matchedCount = currentWeekRequests.stream()
            .filter(r -> r.getStatus() == RequestStatus.MATCHED)
            .count();

        // Total archived requests (all time)
        long archivedCount = requestRepository.findByStatus(RequestStatus.ARCHIVED).size();

        // Cancellations in last 7 days
        long cancellations7d = requestRepository.findByCancelledAfter(sevenDaysAgo).size();

        return new AdminSummary(pendingCount, notMatchedCount, matchedCount, archivedCount, cancellations7d);
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

    /**
     * Summary statistics for admin dashboard.
     */
    public static class AdminSummary {
        private final long pendingCount;
        private final long notMatchedCount;
        private final long matchedCount;
        private final long archivedCount;
        private final long cancellations7d;

        public AdminSummary(long pendingCount, long notMatchedCount, long matchedCount, 
                           long archivedCount, long cancellations7d) {
            this.pendingCount = pendingCount;
            this.notMatchedCount = notMatchedCount;
            this.matchedCount = matchedCount;
            this.archivedCount = archivedCount;
            this.cancellations7d = cancellations7d;
        }

        public long getPendingCount() { return pendingCount; }
        public long getNotMatchedCount() { return notMatchedCount; }
        public long getMatchedCount() { return matchedCount; }
        public long getArchivedCount() { return archivedCount; }
        public long getCancellations7d() { return cancellations7d; }
    }
}