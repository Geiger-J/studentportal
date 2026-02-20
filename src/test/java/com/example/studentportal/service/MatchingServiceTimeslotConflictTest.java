package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.repository.SubjectRepository;
import com.example.studentportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that users cannot be matched multiple times at the same timeslot.
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

        tutor = new User("Tutor Student", "tutor@example.edu", "hashedpass", "STUDENT");
        tutor.setYearGroup(12);
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@example.edu", "hashedpass", "STUDENT");
        tutee.setYearGroup(11);
        tutee = userRepository.save(tutee);

        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);

        physicsSubject = new Subject("PHYSICS", "Physics");
        physicsSubject = subjectRepository.save(physicsSubject);
    }

    @Test
    void testUsersShouldNotBeMatchedAtSameTimeslotMultipleTimes() {
        Set<String> slots = Set.of("MON_P1");

        Request tutorMathRequest = new Request(tutor, "TUTOR", mathSubject, slots);
        Request tuteeMathRequest = new Request(tutee, "TUTEE", mathSubject, slots);
        Request tutorPhysicsRequest = new Request(tutor, "TUTOR", physicsSubject, slots);
        Request tuteePhysicsRequest = new Request(tutee, "TUTEE", physicsSubject, slots);

        requestRepository.save(tutorMathRequest);
        requestRepository.save(tuteeMathRequest);
        requestRepository.save(tutorPhysicsRequest);
        requestRepository.save(tuteePhysicsRequest);

        matchingService.performMatching();

        Request updatedTutorMath = requestRepository.findById(tutorMathRequest.getId()).orElse(null);
        Request updatedTuteeMath = requestRepository.findById(tuteeMathRequest.getId()).orElse(null);
        Request updatedTutorPhysics = requestRepository.findById(tutorPhysicsRequest.getId()).orElse(null);
        Request updatedTuteePhysics = requestRepository.findById(tuteePhysicsRequest.getId()).orElse(null);

        assertNotNull(updatedTutorMath);
        assertNotNull(updatedTuteeMath);
        assertNotNull(updatedTutorPhysics);
        assertNotNull(updatedTuteePhysics);

        int matchedCount = 0;
        if ("MATCHED".equals(updatedTutorMath.getStatus())) matchedCount++;
        if ("MATCHED".equals(updatedTuteeMath.getStatus())) matchedCount++;
        if ("MATCHED".equals(updatedTutorPhysics.getStatus())) matchedCount++;
        if ("MATCHED".equals(updatedTuteePhysics.getStatus())) matchedCount++;

        boolean hasConflict = false;
        String conflictDetails = "";

        if ("MATCHED".equals(updatedTutorMath.getStatus()) &&
            "MATCHED".equals(updatedTutorPhysics.getStatus())) {
            String mathSlot = updatedTutorMath.getChosenTimeslot();
            String physicsSlot = updatedTutorPhysics.getChosenTimeslot();
            if (mathSlot != null && mathSlot.equals(physicsSlot)) {
                hasConflict = true;
                conflictDetails = String.format(
                    "Tutor %s is matched at %s for both Math (with %s) and Physics (with %s)",
                    tutor.getFullName(), mathSlot,
                    updatedTutorMath.getMatchedPartner().getFullName(),
                    updatedTutorPhysics.getMatchedPartner().getFullName()
                );
            }
        }

        if ("MATCHED".equals(updatedTuteeMath.getStatus()) &&
            "MATCHED".equals(updatedTuteePhysics.getStatus())) {
            String mathSlot = updatedTuteeMath.getChosenTimeslot();
            String physicsSlot = updatedTuteePhysics.getChosenTimeslot();
            if (mathSlot != null && mathSlot.equals(physicsSlot)) {
                hasConflict = true;
                conflictDetails += String.format(
                    "\nTutee %s is matched at %s for both Math (with %s) and Physics (with %s)",
                    tutee.getFullName(), mathSlot,
                    updatedTuteeMath.getMatchedPartner().getFullName(),
                    updatedTuteePhysics.getMatchedPartner().getFullName()
                );
            }
        }

        System.out.println("\n=== VERIFICATION ===");
        System.out.println("Matched count: " + matchedCount);
        System.out.println("Has timeslot conflict: " + hasConflict);
        if (hasConflict) {
            System.out.println("Conflict details:\n" + conflictDetails);
        }

        assertFalse(hasConflict,
            "Users should not be matched at the same timeslot for different subjects. " + conflictDetails);
    }
}
