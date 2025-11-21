package com.example.studentportal.service;

import com.example.studentportal.model.*;
import com.example.studentportal.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for tutoring request management.
 * Handles request creation, listing, cancellation, and duplicate prevention.
 */
@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Creates a new tutoring request with duplicate checking.
     * Validates request data.
     * 
     * @param user the user creating the request
     * @param type the request type (TUTOR/TUTEE)
     * @param subject the subject for tutoring
     * @param timeslots the selected timeslots (must have at least one)
     * @return the created request
     * @throws IllegalArgumentException if validation fails or duplicate exists
     */
    public Request createRequest(User user, RequestType type, Subject subject, 
                               Set<Timeslot> timeslots) {
        
        // Validate timeslots
        if (timeslots == null || timeslots.isEmpty()) {
            throw new IllegalArgumentException("At least one timeslot must be selected");
        }

        // Check for duplicate active request
        if (hasActiveRequest(user, subject, type)) {
            throw new IllegalArgumentException(
                "You already have an active " + type.getDisplayName().toLowerCase() + 
                " request for " + subject.getDisplayName());
        }

        // Create and save request
        Request request = new Request(user, type, subject, timeslots);
        return requestRepository.save(request);
    }

    /**
     * Checks if user has an active (PENDING) request for the given subject and type.
     * Used to prevent duplicate requests.
     * 
     * @param user the user
     * @param subject the subject
     * @param type the request type
     * @return true if active request exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveRequest(User user, Subject subject, RequestType type) {
        return requestRepository.existsByUserAndSubjectAndTypeAndStatus(
            user, subject, type, RequestStatus.PENDING);
    }

    /**
     * Retrieves all requests for a specific user, ordered by creation date.
     * 
     * @param user the user whose requests to retrieve
     * @return list of user's requests
     */
    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user) {
        return requestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Cancels a request if it's currently pending.
     * 
     * @param requestId the ID of the request to cancel
     * @param user the user attempting to cancel (for authorization)
     * @return the updated request
     * @throws IllegalArgumentException if request not found, not owned by user, or cannot be cancelled
     */
    public Request cancelRequest(Long requestId, User user) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Request not found");
        }

        Request request = requestOpt.get();

        // Check ownership
        if (!request.getUser().equals(user)) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        // Check if cancellable
        if (!request.canBeCancelled()) {
            throw new IllegalArgumentException("This request cannot be cancelled");
        }

        // Cancel and save
        request.cancel();
        return requestRepository.save(request);
    }

    /**
     * Finds a request by ID.
     * 
     * @param id the request ID
     * @return Optional containing the request if found
     */
    @Transactional(readOnly = true)
    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Retrieves all requests with a specific status.
     * 
     * @param status the status to filter by
     * @return list of requests with the given status
     */
    @Transactional(readOnly = true)
    public List<Request> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    /**
     * Retrieves all pending requests (for future matching algorithm).
     * 
     * @return list of pending requests
     */
    @Transactional(readOnly = true)
    public List<Request> getPendingRequests() {
        return getRequestsByStatus(RequestStatus.PENDING);
    }

    /**
     * Retrieves all non-archived requests.
     * 
     * @return list of requests that are not archived
     */
    @Transactional(readOnly = true)
    public List<Request> getAllNonArchivedRequests() {
        return requestRepository.findAllByArchivedFalse();
    }

    /**
     * Retrieves all matched requests.
     * 
     * @return list of matched requests
     */
    @Transactional(readOnly = true)
    public List<Request> getMatchedRequests() {
        return getRequestsByStatus(RequestStatus.MATCHED);
    }
}