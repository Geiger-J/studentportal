package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.util.DateUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// Service: weighted bipartite matching for tutoring requests
//
// - build a timeslot-level bipartite graph from PENDING requests
// - run max-weight bipartite matching and persist matched pairs
// - enforce hard constraints (subject, year, uniqueness) and soft weights
@Service
@Transactional
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);

    private final RequestRepository requestRepository;
    private final TimeService timeService;

    @Autowired
    public MatchingService(RequestRepository requestRepository, TimeService timeService) {
        this.requestRepository = requestRepository;
        this.timeService = timeService;
    }

    /**
     * RequestTimeslot pair - vertex in the bipartite matching graph. Represents a single request pinned to one of its
     * candidate timeslots.
     */
    private static class RequestTimeslot {
        private final Request request;
        private final String timeslot;

        public RequestTimeslot(Request request, String timeslot) {
            this.request = request;
            this.timeslot = timeslot;
        }

        public Request getRequest() { return request; }

        public String getTimeslot() { return timeslot; }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RequestTimeslot that = (RequestTimeslot) o;
            return Objects.equals(request.getId(), that.request.getId()) && Objects.equals(timeslot, that.timeslot);
        }

        @Override
        public int hashCode() { return Objects.hash(request.getId(), timeslot); }

        @Override
        public String toString() { return "Request[" + request.getId() + "]@" + timeslot; }
    }

    /**
     * Match result - offer/seek request pair with chosen timeslot and edge weight.
     */
    public static class Match {
        private final Request offerRequest;
        private final Request seekRequest;
        private final String timeslot;
        private final double weight;

        public Match(Request offerRequest, Request seekRequest, String timeslot, double weight) {
            this.offerRequest = offerRequest;
            this.seekRequest = seekRequest;
            this.timeslot = timeslot;
            this.weight = weight;
        }

        // accessors and mutators
        public Request getOfferRequest() { return offerRequest; }

        public Request getSeekRequest() { return seekRequest; }

        public String getTimeslot() { return timeslot; }

        public double getWeight() { return weight; }
    }

    // run matching, persist matched pairs, return count of matched requests
    @Transactional
    public int performMatching() {
        List<Match> matches = runMatching();

        int matchedCount = 0;
        for (Match match : matches) {
            Request offerRequest = match.getOfferRequest();
            Request seekRequest = match.getSeekRequest();
            String chosenTimeslot = match.getTimeslot();

            // set week start to Monday of current week [sessions happen within the matching
            // week]
            java.time.LocalDate weekStart = DateUtil.getMondayOfWeek(timeService.today());

            offerRequest.setStatus("MATCHED");
            offerRequest.setMatchedPartner(seekRequest.getUser());
            offerRequest.setChosenTimeslot(chosenTimeslot);
            offerRequest.setWeekStartDate(weekStart);

            seekRequest.setStatus("MATCHED");
            seekRequest.setMatchedPartner(offerRequest.getUser());
            seekRequest.setChosenTimeslot(chosenTimeslot);
            seekRequest.setWeekStartDate(weekStart);

            requestRepository.save(offerRequest);
            requestRepository.save(seekRequest);

            matchedCount += 2;

            logger.info("Matched tutor {} with tutee {} for subject {} at timeslot {} (weight: {})",
                    offerRequest.getUser().getFullName(), seekRequest.getUser().getFullName(),
                    offerRequest.getSubject().getDisplayName(), chosenTimeslot, match.getWeight());
        }

        return matchedCount;
    }

    // build bipartite graph -> run max-weight matching -> return candidate Match
    // list
    public List<Match> runMatching() {
        List<Request> offerRequests = requestRepository.findByStatus("PENDING").stream()
                .filter(r -> "TUTOR".equals(r.getType())).toList();

        List<Request> seekRequests = requestRepository.findByStatus("PENDING").stream()
                .filter(r -> "TUTEE".equals(r.getType())).toList();

        logger.info("Found {} TUTOR requests and {} TUTEE requests for matching", offerRequests.size(),
                seekRequests.size());

        Graph<RequestTimeslot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        Set<RequestTimeslot> offerVertices = new HashSet<>();
        for (Request offer : offerRequests) {
            for (String timeslot : offer.getTimeslots()) {
                // one vertex per (request, timeslot) pair
                RequestTimeslot rt = new RequestTimeslot(offer, timeslot);
                graph.addVertex(rt);
                offerVertices.add(rt);
            }
        }

        Set<RequestTimeslot> seekVertices = new HashSet<>();
        for (Request seek : seekRequests) {
            for (String timeslot : seek.getTimeslots()) {
                RequestTimeslot rt = new RequestTimeslot(seek, timeslot);
                graph.addVertex(rt);
                seekVertices.add(rt);
            }
        }

        int edgeCount = 0;
        for (RequestTimeslot offerRT : offerVertices) {
            for (RequestTimeslot seekRT : seekVertices) {
                // add edge only when timeslots match and hard constraints pass
                if (Objects.equals(offerRT.getTimeslot(), seekRT.getTimeslot())
                        && meetHardConstraints(offerRT.getRequest(), seekRT.getRequest())) {

                    double weight = calculateWeight(offerRT.getRequest(), seekRT.getRequest());
                    // positive weight = candidate is eligible
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

        MaximumWeightBipartiteMatching<RequestTimeslot, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
                graph, offerVertices, seekVertices);

        Set<DefaultWeightedEdge> matchedEdges = matching.getMatching().getEdges();

        logger.info("Matching algorithm found {} potential timeslot matches", matchedEdges.size());

        List<Match> matches = new ArrayList<>();
        Set<Long> matchedOfferRequestIds = new HashSet<>();
        Set<Long> matchedSeekRequestIds = new HashSet<>();

        // collect timeslots already used by currently matched requests [prevents
        // double-booking]
        Map<Long, Set<String>> userMatchedTimeslots = new HashMap<>();
        List<Request> alreadyMatchedRequests = requestRepository.findByStatus("MATCHED");
        for (Request matchedRequest : alreadyMatchedRequests) {
            String chosenTimeslot = matchedRequest.getChosenTimeslot();
            if (chosenTimeslot != null) {
                Long userId = matchedRequest.getUser().getId();
                userMatchedTimeslots.computeIfAbsent(userId, k -> new HashSet<>()).add(chosenTimeslot);
            }
        }

        logger.debug("Initialized user timeslot tracking with {} already matched requests",
                alreadyMatchedRequests.size());

        List<DefaultWeightedEdge> sortedEdges = new ArrayList<>(matchedEdges);
        // greedy pass: highest-weight edges selected first
        sortedEdges.sort((e1, e2) -> Double.compare(graph.getEdgeWeight(e2), graph.getEdgeWeight(e1)));

        for (DefaultWeightedEdge edge : sortedEdges) {
            RequestTimeslot offerRT = graph.getEdgeSource(edge);
            RequestTimeslot seekRT = graph.getEdgeTarget(edge);

            // Ensure source is offer and target is seek
            if ("TUTEE".equals(offerRT.getRequest().getType())) {
                RequestTimeslot temp = offerRT;
                offerRT = seekRT;
                seekRT = temp;
            }

            Request offerRequest = offerRT.getRequest();
            Request seekRequest = seekRT.getRequest();
            String timeslot = offerRT.getTimeslot();

            Long tutorUserId = offerRequest.getUser().getId();
            Long tuteeUserId = seekRequest.getUser().getId();

            boolean requestsNotMatched = !matchedOfferRequestIds.contains(offerRequest.getId())
                    && !matchedSeekRequestIds.contains(seekRequest.getId());
            boolean noTimeslotConflict = !userMatchedTimeslots.getOrDefault(tutorUserId, Collections.emptySet())
                    .contains(timeslot)
                    && !userMatchedTimeslots.getOrDefault(tuteeUserId, Collections.emptySet()).contains(timeslot);

            // keep each request used once; prevent same user booked twice in same slot
            if (requestsNotMatched && noTimeslotConflict) {
                double weight = graph.getEdgeWeight(edge);
                matches.add(new Match(offerRequest, seekRequest, timeslot, weight));

                matchedOfferRequestIds.add(offerRequest.getId());
                matchedSeekRequestIds.add(seekRequest.getId());

                userMatchedTimeslots.computeIfAbsent(tutorUserId, k -> new HashSet<>()).add(timeslot);
                userMatchedTimeslots.computeIfAbsent(tuteeUserId, k -> new HashSet<>()).add(timeslot);

                logger.debug("Selected match: tutor {} with tutee {} at {} (weight: {})",
                        offerRequest.getUser().getFullName(), seekRequest.getUser().getFullName(), timeslot, weight);
            } else if (!noTimeslotConflict) {
                logger.debug("Skipped match due to timeslot conflict: tutor {} with tutee {} at {} (weight: {})",
                        offerRequest.getUser().getFullName(), seekRequest.getUser().getFullName(), timeslot,
                        graph.getEdgeWeight(edge));
            }
        }

        logger.info("Final match count: {} requests matched", matches.size());

        return matches;
    }

    // base weight 100 + exam board bonus + year-proximity bonus; 0 if hard
    // constraints fail
    private double calculateWeight(Request offer, Request seek) {
        if (!meetHardConstraints(offer, seek)) {
            return 0.0;
        }

        double weight = 100.0; // base score for any valid pair

        User tutor = offer.getUser();
        User tutee = seek.getUser();

        // same exam board -> stronger match [more relevant syllabus knowledge]
        if (tutor.getExamBoard() != null && tutor.getExamBoard().equals(tutee.getExamBoard())
                && !"NONE".equals(tutor.getExamBoard())) {
            weight += 50.0;
        }

        // closer year groups -> better match [tutor's knowledge is more recently
        // relevant]
        int yearDifference = tutor.getYearGroup() - tutee.getYearGroup();
        if (yearDifference == 1) {
            weight += 30.0;
        } else if (yearDifference == 0) {
            weight += 25.0;
        } else if (yearDifference == 2) {
            weight += 20.0;
        } else if (yearDifference == 3) {
            weight += 15.0;
        } else if (yearDifference == 4) {
            weight += 10.0;
        }

        logger.debug("Match weight calculated: tutor={}, tutee={}, weight={}", tutor.getFullName(), tutee.getFullName(),
                weight);

        return weight;
    }

    // returns false if subject, timeslot overlap, year order, or uniqueness
    // constraints not met
    private boolean meetHardConstraints(Request offer, Request seek) {
        User tutor = offer.getUser();
        User tutee = seek.getUser();

        // subjects must be identical [exact code match]
        if (!offer.getSubject().getCode().equals(seek.getSubject().getCode())) {
            logger.debug("Constraint failed: subjects don't match - {} vs {}", offer.getSubject().getDisplayName(),
                    seek.getSubject().getDisplayName());
            return false;
        }

        // at least one overlapping timeslot required
        Set<String> tutorSlots = offer.getTimeslots();
        Set<String> tuteeSlots = seek.getTimeslots();
        if (tutorSlots.stream().noneMatch(tuteeSlots::contains)) {
            logger.debug("Constraint failed: no overlapping timeslots between {} and {}", tutor.getFullName(),
                    tutee.getFullName());
            return false;
        }

        // tutor must be same year or older [peer tutoring: no tutoring down year
        // groups]
        if (tutor.getYearGroup() < tutee.getYearGroup()) {
            logger.debug("Constraint failed: tutor year {} < tutee year {} ({} vs {})", tutor.getYearGroup(),
                    tutee.getYearGroup(), tutor.getFullName(), tutee.getFullName());
            return false;
        }

        // self-match guard
        if (tutor.equals(tutee)) {
            logger.debug("Constraint failed: same user cannot match with themselves");
            return false;
        }

        logger.debug("Hard constraints met for: tutor={}, tutee={}", tutor.getFullName(), tutee.getFullName());
        return true;
    }
}
