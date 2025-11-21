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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for handling matching algorithm and scheduled operations.
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
     * Simple Match class to represent a matching result
     */
    public static class Match {
        private final Request offerRequest;
        private final Request seekRequest;
        private final double weight;

        public Match(Request offerRequest, Request seekRequest, double weight) {
            this.offerRequest = offerRequest;
            this.seekRequest = seekRequest;
            this.weight = weight;
        }

        public Request getOfferRequest() {
            return offerRequest;
        }

        public Request getSeekRequest() {
            return seekRequest;
        }

        public double getWeight() {
            return weight;
        }
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

            // Update status and matched partners
            offerRequest.setStatus(RequestStatus.MATCHED);
            offerRequest.setMatchedPartner(seekRequest.getUser());

            seekRequest.setStatus(RequestStatus.MATCHED);
            seekRequest.setMatchedPartner(offerRequest.getUser());

            requestRepository.save(offerRequest);
            requestRepository.save(seekRequest);

            matchedCount += 2;

            logger.info("Matched tutor {} with tutee {} for subject {} (weight: {})",
                    offerRequest.getUser().getFullName(),
                    seekRequest.getUser().getFullName(),
                    offerRequest.getSubject().getDisplayName(),
                    match.getWeight());
        }

        return matchedCount;
    }

    /**
     * Matching algorithm using weighted bipartite matching.
     * 
     * @return list of Match objects representing optimal matches
     */
    public List<Match> runMatching() {
        List<Request> offerRequests = requestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .filter(r -> r.getType() == RequestType.TUTOR)
                .toList();

        List<Request> seekRequests = requestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .filter(r -> r.getType() == RequestType.TUTEE)
                .toList();

        logger.info("Found {} TUTOR requests and {} TUTEE requests for matching",
                offerRequests.size(), seekRequests.size());

        // Create weighted graph
        Graph<Request, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // Add all offer and seek requests as vertices
        Set<Request> offers = new HashSet<>(offerRequests);
        Set<Request> seeks = new HashSet<>(seekRequests);

        for (Request offer : offers) {
            graph.addVertex(offer);
        }
        for (Request seek : seeks) {
            graph.addVertex(seek);
        }

        // Add edges with weights for valid pairs
        int edgeCount = 0;
        for (Request offer : offers) {
            for (Request seek : seeks) {
                double weight = calculateWeight(offer, seek);
                if (weight > 0) {
                    DefaultWeightedEdge edge = graph.addEdge(offer, seek);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, weight);
                        edgeCount++;
                        logger.debug("Added edge between tutor {} and tutee {} with weight {}",
                                offer.getUser().getFullName(),
                                seek.getUser().getFullName(),
                                weight);
                    }
                }
            }
        }

        logger.info("Created bipartite graph with {} valid edges", edgeCount);

        // Run maximum weight bipartite matching
        MaximumWeightBipartiteMatching<Request, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
                graph, offers, seeks);

        Set<DefaultWeightedEdge> matchedEdges = matching.getMatching().getEdges();

        logger.info("Matching algorithm found {} potential matches", matchedEdges.size());

        // Convert to Match objects
        List<Match> matches = new ArrayList<>();
        for (DefaultWeightedEdge edge : matchedEdges) {
            Request source = graph.getEdgeSource(edge);
            Request target = graph.getEdgeTarget(edge);
            double weight = graph.getEdgeWeight(edge);

            // Ensure source is offer and target is seek
            Request offerRequest = source.getType() == RequestType.TUTOR ? source : target;
            Request seekRequest = source.getType() == RequestType.TUTEE ? source : target;

            matches.add(new Match(offerRequest, seekRequest, weight));
        }

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
        if (tutor.getExamBoard() == tutee.getExamBoard() &&
                tutor.getExamBoard() != ExamBoard.NONE) {
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

        logger.debug("Match weight calculated: tutor={}, tutee={}, weight={}",
                tutor.getFullName(), tutee.getFullName(), weight);

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
            logger.debug("Constraint failed: subjects don't match - {} vs {}",
                    offer.getSubject().getDisplayName(), seek.getSubject().getDisplayName());
            return false;
        }

        // At least one overlapping timeslot
        Set<Timeslot> tutorSlots = offer.getTimeslots();
        Set<Timeslot> tuteeSlots = seek.getTimeslots();
        if (tutorSlots.stream().noneMatch(tuteeSlots::contains)) {
            logger.debug("Constraint failed: no overlapping timeslots between {} and {}",
                    tutor.getFullName(), tutee.getFullName());
            return false;
        }

        // Tutor must be same year or higher year than tutee
        if (tutor.getYearGroup() < tutee.getYearGroup()) {
            logger.debug("Constraint failed: tutor year {} < tutee year {} ({} vs {})",
                    tutor.getYearGroup(), tutee.getYearGroup(),
                    tutor.getFullName(), tutee.getFullName());
            return false;
        }

        // Different users (shouldn't match with themselves)
        if (tutor.equals(tutee)) {
            logger.debug("Constraint failed: same user cannot match with themselves");
            return false;
        }

        logger.debug("Hard constraints met for: tutor={}, tutee={}",
                tutor.getFullName(), tutee.getFullName());
        return true;
    }
}