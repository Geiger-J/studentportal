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
 * Tests for RequestService functionality including duplicate request prevention.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestServiceTest {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private RequestRepository requestRepository;

    private User testUser;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        userRepository.deleteAll();
        subjectRepository.deleteAll();

        testUser = new User("Test Student", "1234@example.edu", "hashedpass", "STUDENT");
        testUser = userRepository.save(testUser);

        testSubject = new Subject("MATHS", "Mathematics");
        testSubject = subjectRepository.save(testSubject);
    }

    @Test
    void testCreateRequest_Successful() {
        Set<String> timeslots = Set.of("MON_P1", "TUE_P2");

        Request request = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        assertNotNull(request);
        assertNotNull(request.getId());
        assertEquals(testUser, request.getUser());
        assertEquals("TUTOR", request.getType());
        assertEquals(testSubject, request.getSubject());
        assertEquals(timeslots, request.getTimeslots());
        assertEquals("PENDING", request.getStatus());
        assertFalse(request.getArchived());
    }

    @Test
    void testCreateRequest_WithEmptyTimeslots_ThrowsException() {
        Set<String> emptyTimeslots = Set.of();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.createRequest(
                testUser, "TUTOR", testSubject, emptyTimeslots
            )
        );

        assertEquals("At least one timeslot must be selected", exception.getMessage());
    }

    @Test
    void testCreateRequest_DuplicateActiveRequest_ThrowsException() {
        Set<String> timeslots = Set.of("MON_P1");

        requestService.createRequest(testUser, "TUTOR", testSubject, timeslots);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.createRequest(
                testUser, "TUTOR", testSubject, timeslots
            )
        );

        assertTrue(exception.getMessage().contains("already have an active"));
        assertTrue(exception.getMessage().contains("Mathematics"));
    }

    @Test
    void testCreateRequest_DifferentType_AllowsMultipleRequests() {
        Set<String> timeslots = Set.of("MON_P1");

        Request tutorRequest = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        Request tuteeRequest = requestService.createRequest(
            testUser, "TUTEE", testSubject, timeslots
        );

        assertNotNull(tutorRequest);
        assertNotNull(tuteeRequest);
        assertNotEquals(tutorRequest.getId(), tuteeRequest.getId());
        assertEquals("TUTOR", tutorRequest.getType());
        assertEquals("TUTEE", tuteeRequest.getType());
    }

    @Test
    void testCancelRequest_Successful() {
        Set<String> timeslots = Set.of("MON_P1");
        Request request = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        Request cancelledRequest = requestService.cancelRequest(request.getId(), testUser);

        assertEquals("CANCELLED", cancelledRequest.getStatus());
        assertFalse(cancelledRequest.canBeCancelled());
    }

    @Test
    void testCancelRequest_NonExistentRequest_ThrowsException() {
        Long nonExistentId = 999L;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.cancelRequest(nonExistentId, testUser)
        );

        assertEquals("Request not found", exception.getMessage());
    }

    @Test
    void testHasActiveRequest() {
        Set<String> timeslots = Set.of("MON_P1");

        boolean hasActiveBefore = requestService.hasActiveRequest(testUser, testSubject, "TUTOR");

        requestService.createRequest(testUser, "TUTOR", testSubject, timeslots);
        boolean hasActiveAfter = requestService.hasActiveRequest(testUser, testSubject, "TUTOR");

        assertFalse(hasActiveBefore);
        assertTrue(hasActiveAfter);
    }

    @Test
    void testArchivedStatus() {
        Set<String> timeslots = Set.of("MON_P1");
        Request request = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        request.setArchived(true);
        requestRepository.save(request);

        Request archivedRequest = requestRepository.findById(request.getId()).orElse(null);
        assertNotNull(archivedRequest);
        assertTrue(archivedRequest.getArchived());
    }

    @Test
    void testMatchedPartnerField() {
        Set<String> timeslots = Set.of("MON_P1");

        User partner = new User("Partner Student", "5678@example.edu", "hashedpass", "STUDENT");
        partner = userRepository.save(partner);

        Request request = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        request.setMatchedPartner(partner);
        request.setStatus("MATCHED");
        requestRepository.save(request);

        Request matchedRequest = requestRepository.findById(request.getId()).orElse(null);
        assertNotNull(matchedRequest);
        assertEquals("MATCHED", matchedRequest.getStatus());
        assertEquals(partner, matchedRequest.getMatchedPartner());
        assertEquals(partner.getId(), matchedRequest.getMatchedPartner().getId());
    }

    @Test
    void testFindAllByStatusNot() {
        Set<String> timeslots = Set.of("MON_P1");

        Request pendingRequest = requestService.createRequest(
            testUser, "TUTOR", testSubject, timeslots
        );

        Request archivedRequest = new Request(testUser, "TUTEE", testSubject, timeslots);
        archivedRequest.setArchived(true);
        requestRepository.save(archivedRequest);

        var nonArchivedRequests = requestRepository.findAllByArchivedFalse();

        assertEquals(1, nonArchivedRequests.size());
        assertEquals("PENDING", nonArchivedRequests.get(0).getStatus());
        assertEquals(pendingRequest.getId(), nonArchivedRequests.get(0).getId());
    }
}
