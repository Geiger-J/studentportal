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
    void testPerformMatching_MultipleSubjects_UsersCannotBeMatchedAtSameTimeslot() {
        // Given: Same users but different subjects, same timeslots
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
        
        // Should create a second match (same users can match on different subjects)
        assertEquals(2, secondMatchCount, "Should match the physics requests");
        
        // Verify the status
        Request updatedTutorMath = requestRepository.findById(tutorMathRequest.getId()).orElse(null);
        Request updatedTuteeMath = requestRepository.findById(tuteeMathRequest.getId()).orElse(null);
        Request updatedTutorPhysics = requestRepository.findById(tutorPhysicsRequest.getId()).orElse(null);
        Request updatedTuteePhysics = requestRepository.findById(tuteePhysicsRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorMath);
        assertNotNull(updatedTuteeMath);
        assertNotNull(updatedTutorPhysics);
        assertNotNull(updatedTuteePhysics);
        
        assertEquals(RequestStatus.MATCHED, updatedTutorMath.getStatus());
        assertEquals(RequestStatus.MATCHED, updatedTuteeMath.getStatus());
        assertEquals(RequestStatus.MATCHED, updatedTutorPhysics.getStatus());
        assertEquals(RequestStatus.MATCHED, updatedTuteePhysics.getStatus());
        
        // Critical check: The chosen timeslots MUST be different
        Timeslot mathSlot = updatedTutorMath.getChosenTimeslot();
        Timeslot physicsSlot = updatedTutorPhysics.getChosenTimeslot();
        
        assertNotNull(mathSlot, "Math request should have a chosen timeslot");
        assertNotNull(physicsSlot, "Physics request should have a chosen timeslot");
        assertNotEquals(mathSlot, physicsSlot, 
            "Users cannot be matched at the same timeslot for different subjects");
    }
}
