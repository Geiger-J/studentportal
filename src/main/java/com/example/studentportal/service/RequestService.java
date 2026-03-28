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

// Service: tutoring request lifecycle management
//
// - create requests with duplicate prevention
// - cancel requests and propagate to matched partner
// - archive completed and cancelled requests
@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository) { this.requestRepository = requestRepository; }

    // validate timeslots and duplicate check -> create and persist request
    public Request createRequest(User user, String type, Subject subject, Set<String> timeslots) {

        if (timeslots == null || timeslots.isEmpty()) {
            throw new IllegalArgumentException("At least one timeslot must be selected");
        }

        // reject duplicate active request for same user/subject/type
        if (hasActiveRequest(user, subject, type)) {
            String typeLabel = "TUTOR".equals(type) ? "offering tutoring" : "seeking tutoring";
            throw new IllegalArgumentException(
                    "You already have an active " + typeLabel + " request for " + subject.getDisplayName());
        }

        Request request = new Request(user, type, subject, timeslots);
        return requestRepository.save(request);
    }

    // true if user already has a PENDING request for the given subject and type
    @Transactional(readOnly = true)
    public boolean hasActiveRequest(User user, Subject subject, String type) {
        return requestRepository.existsByUserAndSubjectAndTypeAndStatus(user, subject, type, "PENDING");
    }

    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user) { return requestRepository.findByUserOrderByCreatedAtDesc(user); }

    @Transactional(readOnly = true)
    public List<Request> getUserRequests(User user, boolean includeArchived) {
        if (includeArchived) {
            return requestRepository.findByUserOrderByCreatedAtDesc(user);
        } else {
            return requestRepository.findByUserAndArchivedFalseOrderByCreatedAtDesc(user);
        }
    }

    // cancel request by ID; if matched, also cancel the partner's linked request
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

        // propagate cancellation to matched partner's request
        if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
            User partner = request.getMatchedPartner();
            Optional<Request> partnerRequestOpt = requestRepository
                    .findByUserAndMatchedPartnerAndStatusAndSubject(partner, user, "MATCHED", request.getSubject());

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
    public List<Request> getRequestsByStatus(String status) { return requestRepository.findByStatus(status); }

    @Transactional(readOnly = true)
    public List<Request> getPendingRequests() { return getRequestsByStatus("PENDING"); }

    @Transactional(readOnly = true)
    public List<Request> getAllNonArchivedRequests() { return requestRepository.findAllByArchivedFalse(); }

    @Transactional(readOnly = true)
    public List<Request> getAllRequests() { return requestRepository.findAllByOrderByCreatedAtDesc(); }

    @Transactional(readOnly = true)
    public List<Request> getNonArchivedRequestsByStatus(String status) {
        return requestRepository.findByStatusAndArchivedFalse(status);
    }

    // admin cancel: guard then propagate to matched partner if applicable
    public Request adminCancelRequest(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (!request.canBeCancelled()) {
            throw new IllegalArgumentException("Only pending or matched requests can be cancelled");
        }

        // propagate to partner's matched request [partner sees CANCELLED on their
        // dashboard]
        if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
            User partner = request.getMatchedPartner();
            Optional<Request> partnerOpt = requestRepository.findByUserAndMatchedPartnerAndStatusAndSubject(partner,
                    request.getUser(), "MATCHED", request.getSubject());
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
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
        requestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<Request> getMatchedRequests() { return getRequestsByStatus("MATCHED"); }

    // mark all un-archived DONE and CANCELLED requests as archived; return count
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
