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
 * Test to demonstrate the bug where users can be matched multiple times at the same timeslot
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingServiceTimeslotConflictTest {

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

        tutor = new User("Tutor Student", "tutor@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutor.setYearGroup(12);
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        tutee.setYearGroup(11);
        tutee = userRepository.save(tutee);

        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);
        
        physicsSubject = new Subject("PHYSICS", "Physics");
        physicsSubject = subjectRepository.save(physicsSubject);
    }

    @Test
    void testBug_UsersShouldNotBeMatchedAtSameTimeslotMultipleTimes() {
        // Given: Both users have requests for Math and Physics at the SAME SINGLE timeslot
        Set<Timeslot> slots = Set.of(Timeslot.MON_P1);
        
        // Math requests
        Request tutorMathRequest = new Request(tutor, RequestType.TUTOR, mathSubject, slots);
        Request tuteeMathRequest = new Request(tutee, RequestType.TUTEE, mathSubject, slots);
        
        // Physics requests  
        Request tutorPhysicsRequest = new Request(tutor, RequestType.TUTOR, physicsSubject, slots);
        Request tuteePhysicsRequest = new Request(tutee, RequestType.TUTEE, physicsSubject, slots);
        
        requestRepository.save(tutorMathRequest);
        requestRepository.save(tuteeMathRequest);
        requestRepository.save(tutorPhysicsRequest);
        requestRepository.save(tuteePhysicsRequest);

        // When: Run matching
        matchingService.performMatching();

        // Then: Check if users were matched at the same timeslot for different subjects
        Request updatedTutorMath = requestRepository.findById(tutorMathRequest.getId()).orElse(null);
        Request updatedTuteeMath = requestRepository.findById(tuteeMathRequest.getId()).orElse(null);
        Request updatedTutorPhysics = requestRepository.findById(tutorPhysicsRequest.getId()).orElse(null);
        Request updatedTuteePhysics = requestRepository.findById(tuteePhysicsRequest.getId()).orElse(null);
        
        assertNotNull(updatedTutorMath);
        assertNotNull(updatedTuteeMath);
        assertNotNull(updatedTutorPhysics);
        assertNotNull(updatedTuteePhysics);
        
        // Count how many are matched
        int matchedCount = 0;
        if (updatedTutorMath.getStatus() == RequestStatus.MATCHED) matchedCount++;
        if (updatedTuteeMath.getStatus() == RequestStatus.MATCHED) matchedCount++;
        if (updatedTutorPhysics.getStatus() == RequestStatus.MATCHED) matchedCount++;
        if (updatedTuteePhysics.getStatus() == RequestStatus.MATCHED) matchedCount++;
        
        // Check for timeslot conflicts
        boolean hasConflict = false;
        String conflictDetails = "";
        
        if (updatedTutorMath.getStatus() == RequestStatus.MATCHED && 
            updatedTutorPhysics.getStatus() == RequestStatus.MATCHED) {
            Timeslot mathSlot = updatedTutorMath.getChosenTimeslot();
            Timeslot physicsSlot = updatedTutorPhysics.getChosenTimeslot();
            if (mathSlot == physicsSlot) {
                hasConflict = true;
                conflictDetails = String.format(
                    "Tutor %s is matched at %s for both Math (with %s) and Physics (with %s)",
                    tutor.getFullName(), mathSlot,
                    updatedTutorMath.getMatchedPartner().getFullName(),
                    updatedTutorPhysics.getMatchedPartner().getFullName()
                );
            }
        }
        
        if (updatedTuteeMath.getStatus() == RequestStatus.MATCHED && 
            updatedTuteePhysics.getStatus() == RequestStatus.MATCHED) {
            Timeslot mathSlot = updatedTuteeMath.getChosenTimeslot();
            Timeslot physicsSlot = updatedTuteePhysics.getChosenTimeslot();
            if (mathSlot == physicsSlot) {
                hasConflict = true;
                conflictDetails += String.format(
                    "\nTutee %s is matched at %s for both Math (with %s) and Physics (with %s)",
                    tutee.getFullName(), mathSlot,
                    updatedTuteeMath.getMatchedPartner().getFullName(),
                    updatedTuteePhysics.getMatchedPartner().getFullName()
                );
            }
        }
        
        // This test documents the bug - users CAN be matched at the same timeslot multiple times
        System.out.println("\n=== BUG DEMONSTRATION ===");
        System.out.println("Matched count: " + matchedCount);
        System.out.println("Has timeslot conflict: " + hasConflict);
        if (hasConflict) {
            System.out.println("Conflict details:\n" + conflictDetails);
        }
        
        // This assertion will FAIL, demonstrating the bug exists
        assertFalse(hasConflict, 
            "BUG: Users should not be matched at the same timeslot for different subjects. " + conflictDetails);
    }
}
