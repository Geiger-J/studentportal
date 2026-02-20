package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
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
     *
     * @param user the user creating the request
     * @param type the request type ("TUTOR"/"TUTEE")
     * @param subject the subject for tutoring
     * @param timeslots the selected timeslots (must have at least one)
     * @return the created request
     * @throws IllegalArgumentException if validation fails or duplicate exists
     */
    public Request createRequest(User user, String type, Subject subject,
                               Set<String> timeslots) {

        // Validate timeslots
        if (timeslots == null || timeslots.isEmpty()) {
            throw new IllegalArgumentException("At least one timeslot must be selected");
        }

        // Check for duplicate active request
        if (hasActiveRequest(user, subject, type)) {
            String typeLabel = "TUTOR".equals(type) ? "offering tutoring" : "seeking tutoring";
            throw new IllegalArgumentException(
                "You already have an active " + typeLabel +
                " request for " + subject.getDisplayName());
        }

        // Create and save request
        Request request = new Request(user, type, subject, timeslots);
        return requestRepository.save(request);
    }

    /**
     * Checks if user has an active (PENDING) request for the given subject and type.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveRequest(User user, Subject subject, String type) {
        return requestRepository.existsByUserAndSubjectAndTypeAndStatus(
            user, subject, type, "PENDING");
    }

    /**
     * Retrieves all requests for a specific user, ordered by creation date.
     */
    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user) {
        return requestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Cancels a request if it's currently pending or matched.
     * If the request is matched, also cancels the matched partner's request.
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

        // If request is matched, also cancel the partner's request
        if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
            User partner = request.getMatchedPartner();
            Optional<Request> partnerRequestOpt = requestRepository.findByUserAndMatchedPartnerAndStatusAndSubject(
                partner, user, "MATCHED", request.getSubject());

            if (partnerRequestOpt.isPresent()) {
                Request partnerRequest = partnerRequestOpt.get();
                partnerRequest.cancel();
                requestRepository.save(partnerRequest);
            }
        }

        // Cancel and save the original request
        request.cancel();
        return requestRepository.save(request);
    }

    /**
     * Finds a request by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Retrieves all requests with a specific status.
     */
    @Transactional(readOnly = true)
    public List<Request> getRequestsByStatus(String status) {
        return requestRepository.findByStatus(status);
    }

    /**
     * Retrieves all pending requests.
     */
    @Transactional(readOnly = true)
    public List<Request> getPendingRequests() {
        return getRequestsByStatus("PENDING");
    }

    /**
     * Retrieves all non-archived requests.
     */
    @Transactional(readOnly = true)
    public List<Request> getAllNonArchivedRequests() {
        return requestRepository.findAllByArchivedFalse();
    }

    /**
     * Retrieves all matched requests.
     */
    @Transactional(readOnly = true)
    public List<Request> getMatchedRequests() {
        return getRequestsByStatus("MATCHED");
    }

    /**
     * Archives old DONE and CANCELLED requests.
     */
    public int archiveOldRequests() {
        List<Request> doneRequests = getRequestsByStatus("DONE");
        List<Request> cancelledRequests = getRequestsByStatus("CANCELLED");

        int archivedCount = 0;
        List<Request> requestsToArchive = new java.util.ArrayList<>();

        for (Request request : doneRequests) {
            if (!request.getArchived()) {
                request.setArchived(true);
                requestsToArchive.add(request);
                archivedCount++;
            }
        }

        for (Request request : cancelledRequests) {
            if (!request.getArchived()) {
                request.setArchived(true);
                requestsToArchive.add(request);
                archivedCount++;
            }
        }

        if (!requestsToArchive.isEmpty()) {
            requestRepository.saveAll(requestsToArchive);
        }

        return archivedCount;
    }
}
