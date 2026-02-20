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
 * Test to verify matching algorithm behavior when run multiple times.
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
    void testPerformMatching_RunTwice_ShouldNotRematchAlreadyMatchedRequests() {
        Set<String> slots = Set.of("MON_P1");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, slots);
        Request tuteeRequest = new Request(tutee, "TUTEE", mathSubject, slots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        int firstRun = matchingService.performMatching();
        int secondRun = matchingService.performMatching();

        assertEquals(2, firstRun, "First run should match 2 requests");
        assertEquals(0, secondRun, "Second run should match 0 requests (already matched)");
    }

    @Test
    void testPerformMatching_MultipleSubjects_UsersCannotBeMatchedAtSameTimeslot() {
        Set<String> slots = Set.of("MON_P1", "TUE_P2");

        Request tutorMathRequest = new Request(tutor, "TUTOR", mathSubject, slots);
        Request tuteeMathRequest = new Request(tutee, "TUTEE", mathSubject, slots);

        requestRepository.save(tutorMathRequest);
        requestRepository.save(tuteeMathRequest);

        int matchedCount = matchingService.performMatching();
        assertEquals(2, matchedCount);

        Request tutorPhysicsRequest = new Request(tutor, "TUTOR", physicsSubject, slots);
        Request tuteePhysicsRequest = new Request(tutee, "TUTEE", physicsSubject, slots);

        requestRepository.save(tutorPhysicsRequest);
        requestRepository.save(tuteePhysicsRequest);

        int secondMatchCount = matchingService.performMatching();
        assertEquals(2, secondMatchCount, "Should match the physics requests");

        Request updatedTutorMath = requestRepository.findById(tutorMathRequest.getId()).orElse(null);
        Request updatedTuteeMath = requestRepository.findById(tuteeMathRequest.getId()).orElse(null);
        Request updatedTutorPhysics = requestRepository.findById(tutorPhysicsRequest.getId()).orElse(null);
        Request updatedTuteePhysics = requestRepository.findById(tuteePhysicsRequest.getId()).orElse(null);

        assertNotNull(updatedTutorMath);
        assertNotNull(updatedTuteeMath);
        assertNotNull(updatedTutorPhysics);
        assertNotNull(updatedTuteePhysics);

        assertEquals("MATCHED", updatedTutorMath.getStatus());
        assertEquals("MATCHED", updatedTuteeMath.getStatus());
        assertEquals("MATCHED", updatedTutorPhysics.getStatus());
        assertEquals("MATCHED", updatedTuteePhysics.getStatus());

        String mathSlot = updatedTutorMath.getChosenTimeslot();
        String physicsSlot = updatedTutorPhysics.getChosenTimeslot();

        assertNotNull(mathSlot, "Math request should have a chosen timeslot");
        assertNotNull(physicsSlot, "Physics request should have a chosen timeslot");
        assertNotEquals(mathSlot, physicsSlot,
            "Users cannot be matched at the same timeslot for different subjects");
    }
}
