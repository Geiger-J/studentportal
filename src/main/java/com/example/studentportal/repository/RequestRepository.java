package com.example.studentportal.repository;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.RequestStatus;
import com.example.studentportal.model.RequestType;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Finds all requests excluding archived ones using the archived flag.
     * @return list of requests that are not archived
     */
    List<Request> findAllByArchivedFalse();
    
    /**
     * Deletes all requests associated with a specific user.
     * Used when deleting a user to maintain data integrity.
     * @param user the user whose requests should be deleted
     */
    void deleteByUser(User user);
    
    /**
     * Updates requests to clear matched partner references when a user is deleted.
     * Used to prevent foreign key constraint violations.
     * @param matchedPartner the user to remove as matched partner
     */
    @Modifying
    @Query("UPDATE Request r SET r.matchedPartner = null WHERE r.matchedPartner = :matchedPartner")
    void clearMatchedPartnerReferences(@Param("matchedPartner") User matchedPartner);
    
    /**
     * Finds a matched request for a given user and matched partner.
     * Used when cancelling matched requests to find the partner's request.
     * @param user the user who owns the request
     * @param matchedPartner the matched partner to look for
     * @param status the status to filter by (typically MATCHED)
     * @return Optional containing the matched request if found
     */
    Optional<Request> findByUserAndMatchedPartnerAndStatus(
        User user, User matchedPartner, RequestStatus status);
}