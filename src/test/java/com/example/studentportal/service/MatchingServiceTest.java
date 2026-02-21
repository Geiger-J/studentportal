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
    void testPerformMatching_SuccessfulMatch() {
        Set<String> overlappingSlots = Set.of("MON_P1", "TUE_P2");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, "TUTEE", mathSubject, overlappingSlots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        int matchedCount = matchingService.performMatching();

        assertEquals(2, matchedCount);

        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);

        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);

        assertEquals("MATCHED", updatedTutorRequest.getStatus());
        assertEquals("MATCHED", updatedTuteeRequest.getStatus());

        assertEquals(tutee, updatedTutorRequest.getMatchedPartner());
        assertEquals(tutor, updatedTuteeRequest.getMatchedPartner());
    }

    @Test
    void testPerformMatching_NoOverlappingTimeslots() {
        Set<String> tutorSlots = Set.of("MON_P1", "TUE_P2");
        Set<String> tuteeSlots = Set.of("WED_P3", "THU_P4");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, tutorSlots);
        Request tuteeRequest = new Request(tutee, "TUTEE", mathSubject, tuteeSlots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        int matchedCount = matchingService.performMatching();

        assertEquals(0, matchedCount);

        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);

        assertNotNull(updatedTutorRequest);
        assertNotNull(updatedTuteeRequest);

        assertEquals("PENDING", updatedTutorRequest.getStatus());
        assertEquals("PENDING", updatedTuteeRequest.getStatus());

        assertNull(updatedTutorRequest.getMatchedPartner());
        assertNull(updatedTuteeRequest.getMatchedPartner());
    }

    @Test
    void testPerformMatching_DifferentSubjects() {
        Subject scienceSubject = new Subject("SCIENCE", "Science");
        scienceSubject = subjectRepository.save(scienceSubject);

        Set<String> overlappingSlots = Set.of("MON_P1", "TUE_P2");

        Request tutorRequest = new Request(tutor, "TUTOR", mathSubject, overlappingSlots);
        Request tuteeRequest = new Request(tutee, "TUTEE", scienceSubject, overlappingSlots);

        requestRepository.save(tutorRequest);
        requestRepository.save(tuteeRequest);

        int matchedCount = matchingService.performMatching();

        assertEquals(0, matchedCount);

        Request updatedTutorRequest = requestRepository.findById(tutorRequest.getId()).orElse(null);
        Request updatedTuteeRequest = requestRepository.findById(tuteeRequest.getId()).orElse(null);

        assertEquals("PENDING", updatedTutorRequest.getStatus());
        assertEquals("PENDING", updatedTuteeRequest.getStatus());
    }
}
