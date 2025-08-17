package com.example.studentportal.service;

import com.example.studentportal.model.*;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.repository.SubjectRepository;
import com.example.studentportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MatchingService functionality including matching algorithm and archival.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingServiceTest {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private RequestRepository requestRepository;

    private User tutor;
    private User tutee;
    private Subject mathSubject;
    private LocalDate nextMonday;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        userRepository.deleteAll();
        subjectRepository.deleteAll();

        // Create test users
        tutor = new User("Tutor Student", "tutor@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutee = userRepository.save(tutee);

        // Create test subject
        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);

        // Get current Monday (start of current week)
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // Monday = 1, Sunday = 7
        nextMonday = today.minusDays(dayOfWeek - 1);
    }

    @Test
    void testPerformMatching_SuccessfulMatch() {
        // Given
        Set<Timeslot> overlappingSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, overlappingSlots, nextMonday);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        // When
        int matchedCount = matchingService.performMatching();

        // Then
        assertEquals(2, matchedCount);
        
        // Check that both requests are now matched
        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);
        
        assertEquals(RequestStatus.MATCHED, updatedTutorRequest.getStatus());
        assertEquals(RequestStatus.MATCHED, updatedTuteeRequest.getStatus());
        
        assertEquals(tutee, updatedTutorRequest.getMatchedPartner());
        assertEquals(tutor, updatedTuteeRequest.getMatchedPartner());
    }

    @Test
    void testPerformMatching_NoOverlappingTimeslots() {
        // Given
        Set<Timeslot> tutorSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        Set<Timeslot> tuteeSlots = Set.of(Timeslot.WED_P3, Timeslot.THU_P4);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, tutorSlots, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, tuteeSlots, nextMonday);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        // When
        int matchedCount = matchingService.performMatching();

        // Then
        assertEquals(0, matchedCount);
        
        // Check that both requests are still pending
        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);
        
        assertEquals(RequestStatus.PENDING, updatedTutorRequest.getStatus());
        assertEquals(RequestStatus.PENDING, updatedTuteeRequest.getStatus());
        
        assertNull(updatedTutorRequest.getMatchedPartner());
        assertNull(updatedTuteeRequest.getMatchedPartner());
    }

    @Test
    void testPerformMatching_DifferentSubjects() {
        // Given
        Subject scienceSubject = new Subject("SCIENCE", "Science");
        scienceSubject = subjectRepository.save(scienceSubject);
        
        Set<Timeslot> overlappingSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, scienceSubject, overlappingSlots, nextMonday);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        // When
        int matchedCount = matchingService.performMatching();

        // Then
        assertEquals(0, matchedCount);
        
        // Both requests should remain pending
        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);
        
        assertEquals(RequestStatus.PENDING, updatedTutorRequest.getStatus());
        assertEquals(RequestStatus.PENDING, updatedTuteeRequest.getStatus());
    }

    @Test
    void testPerformArchival() {
        // Given - Create requests with different week start dates
        LocalDate currentWeek = nextMonday; // This week's Monday
        LocalDate lastWeek = currentWeek.minusWeeks(1); // Last week's Monday
        
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);
        
        // Old pending request (should be archived)
        Request oldPendingRequest = new Request(tutor, RequestType.TUTOR, mathSubject, timeslots, lastWeek);
        oldPendingRequest = requestRepository.save(oldPendingRequest);
        
        // Old completed request (should be archived)
        Request oldCompletedRequest = new Request(tutee, RequestType.TUTEE, mathSubject, timeslots, lastWeek);
        oldCompletedRequest.setStatus(RequestStatus.DONE);
        oldCompletedRequest = requestRepository.save(oldCompletedRequest);
        
        // Current week request (should NOT be archived)
        Request currentRequest = new Request(tutor, RequestType.TUTOR, mathSubject, timeslots, currentWeek);
        currentRequest = requestRepository.save(currentRequest);
        
        // Already matched request from last week (should NOT be archived)
        Request matchedRequest = new Request(tutee, RequestType.TUTEE, mathSubject, timeslots, lastWeek);
        matchedRequest.setStatus(RequestStatus.MATCHED);
        matchedRequest = requestRepository.save(matchedRequest);

        // When
        int archivedCount = matchingService.performArchival();

        // Then
        assertEquals(3, archivedCount);
        
        // Check archived requests
        Request archivedPending = requestRepository.findById(oldPendingRequest.getId()).orElse(null);
        Request archivedCompleted = requestRepository.findById(oldCompletedRequest.getId()).orElse(null);
        Request archivedMatched = requestRepository.findById(matchedRequest.getId()).orElse(null);
        
        assertTrue(archivedPending.getArchived());
        assertTrue(archivedCompleted.getArchived());
        assertTrue(archivedMatched.getArchived());
        // Original statuses should be preserved
        assertEquals(RequestStatus.PENDING, archivedPending.getStatus());
        assertEquals(RequestStatus.DONE, archivedCompleted.getStatus());
        assertEquals(RequestStatus.MATCHED, archivedMatched.getStatus());
        
        // Check non-archived requests
        Request stillCurrent = requestRepository.findById(currentRequest.getId()).orElse(null);
        
        assertFalse(stillCurrent.getArchived());
        assertEquals(RequestStatus.PENDING, stillCurrent.getStatus());
    }
}