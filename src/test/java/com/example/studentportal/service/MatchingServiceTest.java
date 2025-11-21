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

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        userRepository.deleteAll();
        subjectRepository.deleteAll();

        // Create test users
        tutor = new User("Tutor Student", "tutor@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutor.setYearGroup(12); // Set year group for matching constraints
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutee.setYearGroup(11); // Set year group for matching constraints  
        tutee = userRepository.save(tutee);

        // Create test subject
        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);
    }

    @Test
    void testPerformMatching_SuccessfulMatch() {
        // Given
        Set<Timeslot> overlappingSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, overlappingSlots);
        
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
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, tutorSlots);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, tuteeSlots);
        
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
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, scienceSubject, overlappingSlots);
        
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
}