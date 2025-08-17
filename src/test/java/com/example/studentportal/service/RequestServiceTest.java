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

import java.time.LocalDate;
import java.util.List;
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

        // Create test user
        testUser = new User("Test Student", "1234@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        testUser = userRepository.save(testUser);

        // Create test subject
        testSubject = new Subject("MATHS", "Mathematics");
        testSubject = subjectRepository.save(testSubject);
    }

    @Test
    void testCreateRequest_Successful() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1, Timeslot.TUE_P2);

        // When
        Request request = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );

        // Then
        assertNotNull(request);
        assertNotNull(request.getId());
        assertEquals(testUser, request.getUser());
        assertEquals(RequestType.TUTOR, request.getType());
        assertEquals(testSubject, request.getSubject());
        assertEquals(timeslots, request.getTimeslots());
        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertNotNull(request.getWeekStartDate());
        assertFalse(request.getArchived());
    }

    @Test
    void testCreateRequest_WithEmptyTimeslots_ThrowsException() {
        // Given
        Set<Timeslot> emptyTimeslots = Set.of();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.createRequest(
                testUser, RequestType.TUTOR, testSubject, emptyTimeslots
            )
        );
        
        assertEquals("At least one timeslot must be selected", exception.getMessage());
    }

    @Test
    void testCreateRequest_DuplicateActiveRequest_ThrowsException() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);

        // Create first request
        requestService.createRequest(testUser, RequestType.TUTOR, testSubject, timeslots);

        // When & Then - Try to create duplicate
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.createRequest(
                testUser, RequestType.TUTOR, testSubject, timeslots
            )
        );
        
        assertTrue(exception.getMessage().contains("already have an active"));
        assertTrue(exception.getMessage().contains("Mathematics"));
    }

    @Test
    void testCreateRequest_DifferentType_AllowsMultipleRequests() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);

        // Create TUTOR request
        Request tutorRequest = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );

        // When - Create TUTEE request for same subject
        Request tuteeRequest = requestService.createRequest(
            testUser, RequestType.TUTEE, testSubject, timeslots
        );

        // Then
        assertNotNull(tutorRequest);
        assertNotNull(tuteeRequest);
        assertNotEquals(tutorRequest.getId(), tuteeRequest.getId());
        assertEquals(RequestType.TUTOR, tutorRequest.getType());
        assertEquals(RequestType.TUTEE, tuteeRequest.getType());
    }

    @Test
    void testCancelRequest_Successful() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);
        Request request = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );

        // When
        Request cancelledRequest = requestService.cancelRequest(request.getId(), testUser);

        // Then
        assertEquals(RequestStatus.CANCELLED, cancelledRequest.getStatus());
        assertFalse(cancelledRequest.canBeCancelled());
    }

    @Test
    void testCancelRequest_NonExistentRequest_ThrowsException() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> requestService.cancelRequest(nonExistentId, testUser)
        );
        
        assertEquals("Request not found", exception.getMessage());
    }

    @Test
    void testHasActiveRequest() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);

        // When - No active request initially
        boolean hasActiveBefore = requestService.hasActiveRequest(testUser, testSubject, RequestType.TUTOR);

        // Create active request
        requestService.createRequest(testUser, RequestType.TUTOR, testSubject, timeslots);
        boolean hasActiveAfter = requestService.hasActiveRequest(testUser, testSubject, RequestType.TUTOR);

        // Then
        assertFalse(hasActiveBefore);
        assertTrue(hasActiveAfter);
    }

    @Test
    void testArchivedStatus() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);
        Request request = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );

        // When - Set archived flag
        request.setArchived(true);
        requestRepository.save(request);

        // Then
        Request archivedRequest = requestRepository.findById(request.getId()).orElse(null);
        assertNotNull(archivedRequest);
        assertTrue(archivedRequest.getArchived());
    }

    @Test
    void testMatchedPartnerField() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);
        
        // Create two users for matching
        User partner = new User("Partner Student", "5678@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        partner = userRepository.save(partner);
        
        Request request = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );

        // When - Set matched partner and status
        request.setMatchedPartner(partner);
        request.setStatus(RequestStatus.MATCHED);
        requestRepository.save(request);

        // Then
        Request matchedRequest = requestRepository.findById(request.getId()).orElse(null);
        assertNotNull(matchedRequest);
        assertEquals(RequestStatus.MATCHED, matchedRequest.getStatus());
        assertEquals(partner, matchedRequest.getMatchedPartner());
        assertEquals(partner.getId(), matchedRequest.getMatchedPartner().getId());
    }

    @Test
    void testFindAllByStatusNot() {
        // Given
        Set<Timeslot> timeslots = Set.of(Timeslot.MON_P1);
        
        // Create requests with different statuses
        Request pendingRequest = requestService.createRequest(
            testUser, RequestType.TUTOR, testSubject, timeslots
        );
        
        Request archivedRequest = new Request(testUser, RequestType.TUTEE, testSubject, timeslots, pendingRequest.getWeekStartDate());
        archivedRequest.setArchived(true);
        requestRepository.save(archivedRequest);

        // When - Find all non-archived requests
        var nonArchivedRequests = requestRepository.findAllByArchivedFalse();

        // Then
        assertEquals(1, nonArchivedRequests.size());
        assertEquals(RequestStatus.PENDING, nonArchivedRequests.get(0).getStatus());
        assertEquals(pendingRequest.getId(), nonArchivedRequests.get(0).getId());
    }
}