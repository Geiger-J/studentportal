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

/*
 * Service – tutoring request lifecycle: creation, retrieval, cancellation, archival
 *
 * Responsibilities:
 * - enforce no-duplicate-active-request rule per user/subject/type
 * - cascade cancellation to matched partner when a MATCHED request is cancelled
 * - archive DONE and CANCELLED requests on demand
 */
@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    // validate → check duplicate → create; throws if timeslots empty or duplicate active request
    public Request createRequest(User user, String type, Subject subject, Set<String> timeslots) {

        if (timeslots == null || timeslots.isEmpty()) {
            throw new IllegalArgumentException("At least one timeslot must be selected");
        }

        if (hasActiveRequest(user, subject, type)) {
            String typeLabel = "TUTOR".equals(type) ? "offering tutoring" : "seeking tutoring";
            throw new IllegalArgumentException("You already have an active " + typeLabel
                    + " request for " + subject.getDisplayName());
        }

        Request request = new Request(user, type, subject, timeslots);
        return requestRepository.save(request);
    }

    // true if user has PENDING request for same subject/type
    @Transactional(readOnly = true)
    public boolean hasActiveRequest(User user, Subject subject, String type) {
        return requestRepository.existsByUserAndSubjectAndTypeAndStatus(user, subject, type,
                "PENDING");
    }

    // all requests for user, newest first
    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user) {
        return requestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // requests for user; includeArchived controls whether archived are included
    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user, boolean includeArchived) {
        if (includeArchived) {
            return requestRepository.findByUserOrderByCreatedAtDesc(user);
        } else {
            return requestRepository.findByUserAndArchivedFalseOrderByCreatedAtDesc(user);
        }
    }

    // cancel request [ownership-checked]; if MATCHED, also cancels partner's request
    public Request cancelRequest(Long requestId, User user) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);

        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Request not found");
        }

        Request request = requestOpt.get();

        if (!request.getUser().equals(user)) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (!request.canBeCancelled()) {
            throw new IllegalArgumentException("This request cannot be cancelled");
        }

        if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
            User partner = request.getMatchedPartner();
            Optional<Request> partnerRequestOpt = requestRepository
                    .findByUserAndMatchedPartnerAndStatusAndSubject(partner, user, "MATCHED",
                            request.getSubject());

            if (partnerRequestOpt.isPresent()) {
                Request partnerRequest = partnerRequestOpt.get();
                partnerRequest.cancel();
                requestRepository.save(partnerRequest);
            }
        }

        request.cancel();
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Optional<Request> findById(Long id) { return requestRepository.findById(id); }

    @Transactional(readOnly = true)
    public List<Request> getRequestsByStatus(String status) {
        return requestRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Request> getPendingRequests() { return getRequestsByStatus("PENDING"); }

    @Transactional(readOnly = true)
    public List<Request> getAllNonArchivedRequests() {
        return requestRepository.findAllByArchivedFalse();
    }

    @Transactional(readOnly = true)
    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Request> getNonArchivedRequestsByStatus(String status) {
        return requestRepository.findByStatusAndArchivedFalse(status);
    }

    // admin cancel — cascades to partner if MATCHED
    public Request adminCancelRequest(Long id) {
        Request request = requestRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Request not found with id: " + id));

        // guard: only pending/matched can be cancelled
        if (!request.canBeCancelled()) {
            throw new IllegalArgumentException("Only pending or matched requests can be cancelled");
        }

        // if matched, cancel the partner's linked request too
        if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
            User partner = request.getMatchedPartner();
            Optional<Request> partnerOpt = requestRepository
                    .findByUserAndMatchedPartnerAndStatusAndSubject(partner, request.getUser(),
                            "MATCHED", request.getSubject());
            if (partnerOpt.isPresent()) {
                Request partnerRequest = partnerOpt.get();
                partnerRequest.cancel();
                requestRepository.save(partnerRequest);
            }
        }

        request.cancel();
        return requestRepository.save(request);
    }

    public void deleteRequest(Long id) {
        Request request = requestRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Request not found with id: " + id));
        requestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<Request> getMatchedRequests() { return getRequestsByStatus("MATCHED"); }

    // mark all non-archived DONE and CANCELLED requests as archived
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
