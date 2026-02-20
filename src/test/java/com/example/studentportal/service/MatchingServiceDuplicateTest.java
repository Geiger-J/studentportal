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

        tutor = new User("Tutor Student", "tutor@example.edu", "hashedpass", "STUDENT");
        tutor.setYearGroup(12);
        tutor = userRepository.save(tutor);

        tutee = new User("Tutee Student", "tutee@example.edu", "hashedpass", "STUDENT");
        tutee.setYearGroup(11);
        tutee = userRepository.save(tutee);

        mathSubject = new Subject("MATHS", "Mathematics");
        mathSubject = subjectRepository.save(mathSubject);
    }

    @Test
    void testPerformMatching_ShouldNotMatchSameUsersTwiceOnDifferentTimeslots() {
        Set<String> overlappingSlots = Set.of("MON_P1", "TUE_P2");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, "TUTEE", mathSubject, overlappingSlots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        int matchedCount = matchingService.performMatching();

        assertEquals(2, matchedCount, "Should match exactly 2 requests (1 pair)");

        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);

        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);

        assertEquals("MATCHED", updatedTutorRequest.getStatus());
        assertEquals("MATCHED", updatedTuteeRequest.getStatus());

        assertEquals(tutee, updatedTutorRequest.getMatchedPartner());
        assertEquals(tutor, updatedTuteeRequest.getMatchedPartner());

        assertNotNull(updatedTutorRequest.getChosenTimeslot(), "Tutor request should have a chosen timeslot");
        assertNotNull(updatedTuteeRequest.getChosenTimeslot(), "Tutee request should have a chosen timeslot");

        assertEquals(updatedTutorRequest.getChosenTimeslot(), updatedTuteeRequest.getChosenTimeslot(),
                "Both matched requests should have the same chosen timeslot");
    }

    @Test
    void testPerformMatching_ThreeUsersWithOverlappingTimeslots() {
        User tutee2 = new User("Second Tutee", "tutee2@example.edu", "hashedpass", "STUDENT");
        tutee2.setYearGroup(10);
        tutee2 = userRepository.save(tutee2);

        Set<String> overlappingSlots = Set.of("MON_P1", "TUE_P2");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, overlappingSlots);
        Request tutee1Request = new Request(tutee, "TUTEE", mathSubject, overlappingSlots);
        Request tutee2Request = new Request(tutee2, "TUTEE", mathSubject, overlappingSlots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tutee1Request);
        requestRepository.save(tutee2Request);

        int matchedCount = matchingService.performMatching();

        assertEquals(2, matchedCount, "Should match exactly 2 requests (1 pair), not both tutees");

        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        assertNotNull(updatedTutorRequest);
        assertEquals("MATCHED", updatedTutorRequest.getStatus());

        Request updatedTutee1Request = requestRepository.findById(tutee1Request.getId()).orElse(null);
        Request updatedTutee2Request = requestRepository.findById(tutee2Request.getId()).orElse(null);

        int matchedTutees = 0;
        if ("MATCHED".equals(updatedTutee1Request.getStatus())) matchedTutees++;
        if ("MATCHED".equals(updatedTutee2Request.getStatus())) matchedTutees++;

        assertEquals(1, matchedTutees, "Exactly one tutee should be matched with the tutor");
    }
}
