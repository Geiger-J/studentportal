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
 * Test to verify that users cannot be matched multiple times with the same partner
 * on different timeslots.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingServiceDuplicateTest {

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
        tutor.setYearGroup(12);
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutee.setYearGroup(11);
        tutee = userRepository.save(tutee);

        // Create test subject
        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);
    }

    @Test
    void testPerformMatching_ShouldNotMatchSameUsersTwiceOnDifferentTimeslots() {
        // Given: Two users with overlapping timeslots MON_P1 and TUE_P2
        Set<Timeslot> overlappingSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, overlappingSlots);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        // When: Matching algorithm runs
        int matchedCount = matchingService.performMatching();

        // Then: Should match exactly once (2 requests matched = 1 match)
        assertEquals(2, matchedCount, "Should match exactly 2 requests (1 pair)");
        
        // Verify both requests are matched
        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);
        
        assertEquals(RequestStatus.MATCHED, updatedTutorRequest.getStatus());
        assertEquals(RequestStatus.MATCHED, updatedTuteeRequest.getStatus());
        
        // Verify they are matched with each other
        assertEquals(tutee, updatedTutorRequest.getMatchedPartner());
        assertEquals(tutor, updatedTuteeRequest.getMatchedPartner());
        
        // Verify only ONE timeslot was chosen for the match
        assertNotNull(updatedTutorRequest.getChosenTimeslot(), "Tutor request should have a chosen timeslot");
        assertNotNull(updatedTuteeRequest.getChosenTimeslot(), "Tutee request should have a chosen timeslot");
        
        // Both should have the same chosen timeslot
        assertEquals(updatedTutorRequest.getChosenTimeslot(), updatedTuteeRequest.getChosenTimeslot(),
                "Both matched requests should have the same chosen timeslot");
    }

    @Test
    void testPerformMatching_ThreeUsersWithOverlappingTimeslots() {
        // Given: Three users where two tutees want the same tutor
        User tutee2 = new User("Second Tutee", "tutee2@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutee2.setYearGroup(10);
        tutee2 = userRepository.save(tutee2);
        
        Set<Timeslot> overlappingSlots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, overlappingSlots);
        Request tutee1Request = new Request(tutee, RequestType.TUTEE, mathSubject, overlappingSlots);
        Request tutee2Request = new Request(tutee2, RequestType.TUTEE, mathSubject, overlappingSlots);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tutee1Request);
        requestRepository.save(tutee2Request);

        // When: Matching algorithm runs
        int matchedCount = matchingService.performMatching();

        // Then: Only ONE pair should be matched (the tutor can only match once)
        assertEquals(2, matchedCount, "Should match exactly 2 requests (1 pair), not both tutees");
        
        // Verify the tutor is matched with exactly one tutee
        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        assertNotNull(updatedTutorRequest);
        assertEquals(RequestStatus.MATCHED, updatedTutorRequest.getStatus());
        
        // Count how many tutees were matched
        Request updatedTutee1Request = requestRepository.findById(tutee1Request.getId()).orElse(null);
        Request updatedTutee2Request = requestRepository.findById(tutee2Request.getId()).orElse(null);
        
        int matchedTutees = 0;
        if (updatedTutee1Request.getStatus() == RequestStatus.MATCHED) matchedTutees++;
        if (updatedTutee2Request.getStatus() == RequestStatus.MATCHED) matchedTutees++;
        
        assertEquals(1, matchedTutees, "Exactly one tutee should be matched with the tutor");
    }
}
