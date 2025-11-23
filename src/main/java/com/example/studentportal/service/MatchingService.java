


package com.example.studentportal.service;

import com.example.studentportal.model.*;
import com.example.studentportal.repository.RequestRepository;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for handling matching algorithm and scheduled operations.
 */
@Service
@Transactional
public class MatchingService {
    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private final RequestRepository requestRepository;

    @Autowired
    public MatchingService(RequestRepository requestRepository) { this.requestRepository = requestRepository; }

    /**
     * RequestTimeslot pair representing a request at a specific timeslot. Used as
     * vertices in the bipartite matching graph.
     */
    private static class RequestTimeslot {
        private final Request request;
        private final Timeslot timeslot;

        public RequestTimeslot(Request request, Timeslot timeslot) {
            this.request = request;
            this.timeslot = timeslot;
        }

        public Request getRequest() { return request; }

        public Timeslot getTimeslot() { return timeslot; }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RequestTimeslot that = (RequestTimeslot) o;
            return Objects.equals(request.getId(), that.request.getId()) && timeslot == that.timeslot;
        }

        @Override
        public int hashCode() { return Objects.hash(request.getId(), timeslot); }

        @Override
        public String toString() { return "Request[" + request.getId() + "]@" + timeslot; }
    }

    /**
     * Simple Match class to represent a matching result
     */
    public static class Match {
        private final Request offerRequest;
        private final Request seekRequest;
        private final Timeslot timeslot;
        private final double weight;

        public Match(Request offerRequest, Request seekRequest, Timeslot timeslot, double weight) {
            this.offerRequest = offerRequest;
            this.seekRequest = seekRequest;
            this.timeslot = timeslot;
            this.weight = weight;
        }

        public Request getOfferRequest() { return offerRequest; }

        public Request getSeekRequest() { return seekRequest; }

        public Timeslot getTimeslot() { return timeslot; }

        public double getWeight() { return weight; }
    }

    /**
     * Manual matching trigger - performs the matching algorithm immediately.
     * 
     * @return number of requests matched
     */
    @Transactional
    public int performMatching() {
        List<Match> matches = runMatching();
        // Process the matches
        int matchedCount = 0;
        for (Match match : matches) {
            Request offerRequest = match.getOfferRequest();
            Request seekRequest = match.getSeekRequest();
            Timeslot chosenTimeslot = match.getTimeslot();
            // Update status and matched partners
            offerRequest.setStatus(RequestStatus.MATCHED);
            offerRequest.setMatchedPartner(seekRequest.getUser());
            offerRequest.setChosenTimeslot(chosenTimeslot);
            seekRequest.setStatus(RequestStatus.MATCHED);
            seekRequest.setMatchedPartner(offerRequest.getUser());
            seekRequest.setChosenTimeslot(chosenTimeslot);
            requestRepository.save(offerRequest);
            requestRepository.save(seekRequest);
            matchedCount += 2;
            logger.info("Matched tutor {} with tutee {} for subject {} at timeslot {} (weight: {})",
                    offerRequest.getUser().getFullName(), seekRequest.getUser().getFullName(),
                    offerRequest.getSubject().getDisplayName(), chosenTimeslot, match.getWeight());
        }
        return matchedCount;
    }

    /**
     * Matching algorithm using weighted bipartite matching at the timeslot level.
     * Each (request, timeslot) pair is treated as a separate vertex. Ensures each
     * request is matched at most once.
     * 
     * @return list of Match objects representing optimal matches
     */
    public List<Match> runMatching() {
        List<Request> offerRequests = requestRepository.findByStatus(RequestStatus.PENDING).stream()
                .filter(r -> r.getType() == RequestType.TUTOR).toList();
        List<Request> seekRequests = requestRepository.findByStatus(RequestStatus.PENDING).stream()
                .filter(r -> r.getType() == RequestType.TUTEE).toList();
        logger.info("Found {} TUTOR requests and {} TUTEE requests for matching", offerRequests.size(),
                seekRequests.size());
        // Create weighted graph with RequestTimeslot vertices
        Graph<RequestTimeslot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        // Create RequestTimeslot vertices for each offer request and its timeslots
        Set<RequestTimeslot> offerVertices = new HashSet<>();
        for (Request offer : offerRequests) {
            for (Timeslot timeslot : offer.getTimeslots()) {
                RequestTimeslot rt = new RequestTimeslot(offer, timeslot);
                graph.addVertex(rt);
                offerVertices.add(rt);
            }
        }
        // Create RequestTimeslot vertices for each seek request and its timeslots
        Set<RequestTimeslot> seekVertices = new HashSet<>();
        for (Request seek : seekRequests) {
            for (Timeslot timeslot : seek.getTimeslots()) {
                RequestTimeslot rt = new RequestTimeslot(seek, timeslot);
                graph.addVertex(rt);
                seekVertices.add(rt);
            }
        }
        // Add edges between compatible RequestTimeslot pairs
        int edgeCount = 0;
        for (RequestTimeslot offerRT : offerVertices) {
            for (RequestTimeslot seekRT : seekVertices) {
                // Edges only exist if:
                // 1. Same timeslot
                // 2. Requests meet hard constraints (subject, year group, etc.)
                if (offerRT.getTimeslot() == seekRT.getTimeslot()
                        && meetHardConstraints(offerRT.getRequest(), seekRT.getRequest())) {
                    double weight = calculateWeight(offerRT.getRequest(), seekRT.getRequest());
                    if (weight > 0) {
                        DefaultWeightedEdge edge = graph.addEdge(offerRT, seekRT);
                        if (edge != null) {
                            graph.setEdgeWeight(edge, weight);
                            edgeCount++;
                        }
                    }
                }
            }
        }
        logger.info("Created bipartite graph with {} offer vertices, {} seek vertices, and {} valid edges",
                offerVertices.size(), seekVertices.size(), edgeCount);
        // Run maximum weight bipartite matching
        MaximumWeightBipartiteMatching<RequestTimeslot, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
                graph, offerVertices, seekVertices);
        Set<DefaultWeightedEdge> matchedEdges = matching.getMatching().getEdges();
        logger.info("Matching algorithm found {} potential timeslot matches", matchedEdges.size());
        // Convert matched edges to Match objects, ensuring each request is matched at
        // most once
        List<Match> matches = new ArrayList<>();
        Set<Long> matchedOfferRequestIds = new HashSet<>();
        Set<Long> matchedSeekRequestIds = new HashSet<>();
        // Sort edges by weight (descending) to prioritize higher-weight matches
        List<DefaultWeightedEdge> sortedEdges = new ArrayList<>(matchedEdges);
        sortedEdges.sort((e1, e2) -> Double.compare(graph.getEdgeWeight(e2), graph.getEdgeWeight(e1)));
        for (DefaultWeightedEdge edge : sortedEdges) {
            RequestTimeslot offerRT = graph.getEdgeSource(edge);
            RequestTimeslot seekRT = graph.getEdgeTarget(edge);
            // Ensure source is offer and target is seek
            if (offerRT.getRequest().getType() == RequestType.TUTEE) {
                RequestTimeslot temp = offerRT;
                offerRT = seekRT;
                seekRT = temp;
            }
            Request offerRequest = offerRT.getRequest();
            Request seekRequest = seekRT.getRequest();
            Timeslot timeslot = offerRT.getTimeslot();
            // Only add match if neither request has been matched yet
            if (!matchedOfferRequestIds.contains(offerRequest.getId())
                    && !matchedSeekRequestIds.contains(seekRequest.getId())) {
                double weight = graph.getEdgeWeight(edge);
                matches.add(new Match(offerRequest, seekRequest, timeslot, weight));
                matchedOfferRequestIds.add(offerRequest.getId());
                matchedSeekRequestIds.add(seekRequest.getId());
                logger.debug("Selected match: tutor {} with tutee {} at {} (weight: {})",
                        offerRequest.getUser().getFullName(), seekRequest.getUser().getFullName(), timeslot, weight);
            }
        }
        logger.info("Final match count: {} requests matched", matches.size());
        return matches;
    }

    /**
     * Calculate weight for a potential match between offer and seek requests.
     * Returns 0 if hard constraints are not met.
     */
    private double calculateWeight(Request offer, Request seek) {
        // Hard constraints
        if (!meetHardConstraints(offer, seek)) {
            return 0.0;
        }
        // Base weight
        double weight = 100.0;
        User tutor = offer.getUser();
        User tutee = seek.getUser();
        // Exam Board bonus
        if (tutor.getExamBoard() == tutee.getExamBoard() && tutor.getExamBoard() != ExamBoard.NONE) {
            weight += 50.0; // Significant bonus for matching exam boards
        }
        // Year Group Difference bonus
        int yearDifference = tutor.getYearGroup() - tutee.getYearGroup();
        if (yearDifference == 1) {
            weight += 30.0; // Best case: tutor is 1 year ahead
        } else if (yearDifference == 0) {
            weight += 25.0; // Same year group
        } else if (yearDifference == 2) {
            weight += 20.0; // 2 years ahead
        } else if (yearDifference == 3) {
            weight += 15.0; // 3 years ahead
        } else if (yearDifference == 4) {
            weight += 10.0; // 4 years ahead
        }
        logger.debug("Match weight calculated: tutor={}, tutee={}, weight={}", tutor.getFullName(), tutee.getFullName(),
                weight);
        return weight;
    }

    /**
     * Check if hard constraints are met for a potential match.
     */
    private boolean meetHardConstraints(Request offer, Request seek) {
        User tutor = offer.getUser();
        User tutee = seek.getUser();
        // Subject must be identical
        if (offer.getSubject().getCode() != seek.getSubject().getCode()) {
            logger.debug("Constraint failed: subjects don't match - {} vs {}", offer.getSubject().getDisplayName(),
                    seek.getSubject().getDisplayName());
            return false;
        }
        // At least one overlapping timeslot
        Set<Timeslot> tutorSlots = offer.getTimeslots();
        Set<Timeslot> tuteeSlots = seek.getTimeslots();
        if (tutorSlots.stream().noneMatch(tuteeSlots::contains)) {
            logger.debug("Constraint failed: no overlapping timeslots between {} and {}", tutor.getFullName(),
                    tutee.getFullName());
            return false;
        }
        // Tutor must be same year or higher year than tutee
        if (tutor.getYearGroup() < tutee.getYearGroup()) {
            logger.debug("Constraint failed: tutor year {} < tutee year {} ({} vs {})", tutor.getYearGroup(),
                    tutee.getYearGroup(), tutor.getFullName(), tutee.getFullName());
            return false;
        }
        // Different users (shouldn't match with themselves)
        if (tutor.equals(tutee)) {
            logger.debug("Constraint failed: same user cannot match with themselves");
            return false;
        }
        logger.debug("Hard constraints met for: tutor={}, tutee={}", tutor.getFullName(), tutee.getFullName());
        return true;
    }
}