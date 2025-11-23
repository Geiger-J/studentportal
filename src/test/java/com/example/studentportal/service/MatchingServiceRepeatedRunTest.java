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
 * Test to verify matching algorithm behavior when run multiple times
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingServiceRepeatedRunTest {

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
    private Subject physicsSubject;

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

        // Create test subjects
        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);
        
        physicsSubject = new Subject("PHYSICS", "Physics");
        physicsSubject = subjectRepository.save(physicsSubject);
    }

    @Test
    void testPerformMatching_RunTwice_ShouldNotRematchAlreadyMatchedRequests() {
        // Given: Initial pair of matching requests
        Set<Timeslot> slots = Set.of(Timeslot.MON_P1);
        
        Request tutorRequest = new Request(tutor, RequestType.TUTOR, mathSubject, slots);
        Request tuteeRequest = new Request(tutee, RequestType.TUTEE, mathSubject, slots);
        
        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        // When: Run matching twice
        int firstRun = matchingService.performMatching();
        int secondRun = matchingService.performMatching();

        // Then: First run should match, second should find nothing to match
        assertEquals(2, firstRun, "First run should match 2 requests");
        assertEquals(0, secondRun, "Second run should match 0 requests (already matched)");
    }

    @Test
    void testPerformMatching_MultipleSubjects_UsersCanBeMatchedOnDifferentSubjects() {
        // Given: Same users but different subjects
        Set<Timeslot> slots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);
        
        // Math requests
        Request tutorMathRequest = new Request(tutor, RequestType.TUTOR, mathSubject, slots);
        Request tuteeMathRequest = new Request(tutee, RequestType.TUTEE, mathSubject, slots);
        
        requestRepository.save(tutorMathRequest);
        requestRepository.save(tuteeMathRequest);

        // When: Run matching
        int matchedCount = matchingService.performMatching();

        // Then: Should match on math
        assertEquals(2, matchedCount);
        
        // Now add physics requests for the same users
        Request tutorPhysicsRequest = new Request(tutor, RequestType.TUTOR, physicsSubject, slots);
        Request tuteePhysicsRequest = new Request(tutee, RequestType.TUTEE, physicsSubject, slots);
        
        requestRepository.save(tutorPhysicsRequest);
        requestRepository.save(tuteePhysicsRequest);
        
        // Run matching again
        int secondMatchCount = matchingService.performMatching();
        
        // Question: Should the same users be matched again on a different subject?
        // Currently, this would create a second match at a different timeslot
        System.out.println("Second match count: " + secondMatchCount);
        
        // Verify the status
        Request updatedTutorPhysics = requestRepository.findById(tutorPhysicsRequest.getId()).orElse(null);
        Request updatedTuteePhysics = requestRepository.findById(tuteePhysicsRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorPhysics);
        assertNotNull(updatedTuteePhysics);
        
        System.out.println("Tutor physics status: " + updatedTutorPhysics.getStatus());
        System.out.println("Tutee physics status: " + updatedTuteePhysics.getStatus());
        
        if (updatedTutorPhysics.getStatus() == RequestStatus.MATCHED) {
            System.out.println("Tutor physics chosen timeslot: " + updatedTutorPhysics.getChosenTimeslot());
            System.out.println("Tutee physics chosen timeslot: " + updatedTuteePhysics.getChosenTimeslot());
        }
    }
}
