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
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots, false, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, overlappingSlots, false, nextMonday);
        
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
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, tutorSlots, false, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, tuteeSlots, false, nextMonday);
        
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
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots, false, nextMonday);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, scienceSubject, overlappingSlots, false, nextMonday);
        
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
        Request oldPendingRequest = new Request(tutor, RequestType.TUTOR, mathSubject, timeslots, false, lastWeek);
        oldPendingRequest = requestRepository.save(oldPendingRequest);
        
        // Old completed request (should be archived)
        Request oldCompletedRequest = new Request(tutee, RequestType.TUTEE, mathSubject, timeslots, false, lastWeek);
        oldCompletedRequest.setStatus(RequestStatus.COMPLETED);
        oldCompletedRequest = requestRepository.save(oldCompletedRequest);
        
        // Current week request (should NOT be archived)
        Request currentRequest = new Request(tutor, RequestType.TUTOR, mathSubject, timeslots, false, currentWeek);
        currentRequest = requestRepository.save(currentRequest);
        
        // Already matched request from last week (should NOT be archived)
        Request matchedRequest = new Request(tutee, RequestType.TUTEE, mathSubject, timeslots, false, lastWeek);
        matchedRequest.setStatus(RequestStatus.MATCHED);
        matchedRequest = requestRepository.save(matchedRequest);

        // When
        int archivedCount = matchingService.performArchival();

        // Then
        assertEquals(2, archivedCount);
        
        // Check archived requests
        Request archivedPending = requestRepository.findById(oldPendingRequest.getId()).orElse(null);
        Request archivedCompleted = requestRepository.findById(oldCompletedRequest.getId()).orElse(null);
        
        assertEquals(RequestStatus.ARCHIVED, archivedPending.getStatus());
        assertEquals(RequestStatus.ARCHIVED, archivedCompleted.getStatus());
        
        // Check non-archived requests
        Request stillCurrent = requestRepository.findById(currentRequest.getId()).orElse(null);
        Request stillMatched = requestRepository.findById(matchedRequest.getId()).orElse(null);
        
        assertEquals(RequestStatus.PENDING, stillCurrent.getStatus());
        assertEquals(RequestStatus.MATCHED, stillMatched.getStatus());
    }
    
    @Test
    void testPerformMatching_ExcludesAdminUsers() {
        // Given - Create admin user
        User adminUser = new User("Admin User", "admin@bromsgrove-school.co.uk", "hashedpass", Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        // Admin user creates a tutor request (shouldn't be matched)
        Request adminTutorRequest = new Request(adminUser, RequestType.TUTOR, mathSubject, timeslots, false, nextMonday);
        adminTutorRequest = requestRepository.save(adminTutorRequest);
        
        // Student creates a tutee request
        Request studentTuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, timeslots, false, nextMonday);
        studentTuteeRequest = requestRepository.save(studentTuteeRequest);

        // When
        int matchedCount = matchingService.performMatching();

        // Then - No matches should occur because admin user is excluded
        assertEquals(0, matchedCount);
        
        // Check that requests are still pending
        Request updatedAdminRequest = requestRepository.findById(adminTutorRequest.getId()).orElse(null);
        Request updatedStudentRequest = requestRepository.findById(studentTuteeRequest.getId()).orElse(null);
        
        assertNotNull(updatedAdminRequest);
        assertNotNull(updatedStudentRequest);
        
        assertEquals(RequestStatus.PENDING, updatedAdminRequest.getStatus());
        assertEquals(RequestStatus.PENDING, updatedStudentRequest.getStatus());
        
        assertNull(updatedAdminRequest.getMatchedPartner());
        assertNull(updatedStudentRequest.getMatchedPartner());
    }
}