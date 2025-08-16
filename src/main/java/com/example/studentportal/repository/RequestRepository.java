package com.example.studentportal.repository;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.RequestStatus;
import com.example.studentportal.model.RequestType;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Request entity operations.
 * Provides database access methods for tutoring request management.
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    
    /**
     * Finds all requests for a specific user, ordered by creation date (newest first).
     * @param user the user whose requests to find
     * @return list of requests for the user
     */
    List<Request> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Finds an active (PENDING) request by user, subject, and type.
     * Used to prevent duplicate active requests.
     * @param user the user
     * @param subject the subject
     * @param type the request type
     * @param status the request status (typically PENDING)
     * @return Optional containing the request if found
     */
    Optional<Request> findByUserAndSubjectAndTypeAndStatus(
        User user, Subject subject, RequestType type, RequestStatus status);
    
    /**
     * Checks if an active request exists for the given user, subject, and type.
     * @param user the user
     * @param subject the subject
     * @param type the request type
     * @param status the request status (typically PENDING)
     * @return true if an active request exists, false otherwise
     */
    boolean existsByUserAndSubjectAndTypeAndStatus(
        User user, Subject subject, RequestType type, RequestStatus status);
    
    /**
     * Finds all requests with a specific status.
     * @param status the status to filter by
     * @return list of requests with the given status
     */
    List<Request> findByStatus(RequestStatus status);
    
    /**
     * Finds all requests for a specific subject.
     * @param subject the subject to filter by
     * @return list of requests for the subject
     */
    List<Request> findBySubject(Subject subject);
    
    /**
     * Finds all requests that are NOT archived.
     * @param status the status to exclude (typically ARCHIVED)
     * @return list of non-archived requests
     */
    List<Request> findAllByStatusNot(RequestStatus status);
    
    /**
     * Finds requests by status and weekStartDate before the given date.
     * Used for archival of old requests.
     * @param statuses list of statuses to include
     * @param weekStartDate the cutoff date
     * @return list of requests matching criteria
     */
    List<Request> findByStatusInAndWeekStartDateBefore(List<RequestStatus> statuses, LocalDate weekStartDate);
    
    /**
     * Finds requests by status and weekStartDate.
     * Used for matching current week requests.
     * @param status the status to filter by
     * @param weekStartDate the week start date
     * @return list of requests matching criteria
     */
    List<Request> findByStatusAndWeekStartDate(RequestStatus status, LocalDate weekStartDate);
}