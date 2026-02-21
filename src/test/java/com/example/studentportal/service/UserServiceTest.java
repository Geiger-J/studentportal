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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserService functionality including registration, role determination, and password hashing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Subject testSubject;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        userRepository.deleteAll();
        subjectRepository.deleteAll();
        testSubject = subjectRepository.save(new Subject("MATHS", "Mathematics"));
    }

    @Test
    void testRegisterUser_WithStudentEmail_AssignsStudentRole() {
        String fullName = "John Smith";
        String email = "1234student@example.edu";
        String password = "testpass123";

        User savedUser = userService.registerUser(fullName, email, password);

        assertNotNull(savedUser);
        assertEquals(fullName, savedUser.getFullName());
        assertEquals(email, savedUser.getEmail());
        assertEquals("STUDENT", savedUser.getRole());

        assertNotEquals(password, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void testRegisterUser_WithStaffEmail_AssignsAdminRole() {
        String fullName = "Jane Teacher";
        String email = "jane.teacher@example.edu";
        String password = "teacherpass";

        User savedUser = userService.registerUser(fullName, email, password);

        assertNotNull(savedUser);
        assertEquals(fullName, savedUser.getFullName());
        assertEquals(email, savedUser.getEmail());
        assertEquals("ADMIN", savedUser.getRole());

        assertNotEquals(password, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void testRegisterUser_WithInvalidEmailDomain_ThrowsException() {
        String fullName = "Invalid User";
        String email = "user@invalid-domain.com";
        String password = "password";

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(fullName, email, password)
        );

        assertEquals("Email must end with @example.edu", exception.getMessage());
    }

    @Test
    void testRegisterUser_WithDuplicateEmail_ThrowsException() {
        String fullName1 = "First User";
        String fullName2 = "Second User";
        String email = "test@example.edu";
        String password = "password";

        userService.registerUser(fullName1, email, password);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(fullName2, email, password)
        );

        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void testDetermineRoleFromEmail_StudentEmails() {
        assertEquals("STUDENT", userService.determineRoleFromEmail("1234@example.edu"));
        assertEquals("STUDENT", userService.determineRoleFromEmail("0123student@example.edu"));
        assertEquals("STUDENT", userService.determineRoleFromEmail("9test@example.edu"));
    }

    @Test
    void testDetermineRoleFromEmail_AdminEmails() {
        assertEquals("ADMIN", userService.determineRoleFromEmail("teacher@example.edu"));
        assertEquals("ADMIN", userService.determineRoleFromEmail("admin@example.edu"));
        assertEquals("ADMIN", userService.determineRoleFromEmail("john.smith@example.edu"));
    }

    @Test
    void testIsProfileComplete_IncompleteProfile() {
        User user = new User("Test User", "test@example.edu", "hashedpass", "STUDENT");

        assertFalse(userService.isProfileComplete(user));
        assertFalse(user.isProfileComplete());
    }

    // ── deleteUser partner-cancellation tests ──────────────────────────────

    @Test
    void testDeleteUser_CancelsPartnerMatchedRequest() {
        // set up two matched users
        User userA = userRepository.save(new User("User A", "1111@example.edu", "pass", "STUDENT"));
        User userB = userRepository.save(new User("User B", "2222@example.edu", "pass", "STUDENT"));

        // create a MATCHED request pair
        Request reqA = new Request(userA, "TUTOR", testSubject, Set.of("MON_P1"));
        reqA.setStatus("MATCHED");
        reqA.setMatchedPartner(userB);
        requestRepository.save(reqA);

        Request reqB = new Request(userB, "TUTEE", testSubject, Set.of("MON_P1"));
        reqB.setStatus("MATCHED");
        reqB.setMatchedPartner(userA);
        requestRepository.save(reqB);

        // delete userA — userB's request should be cancelled
        userService.deleteUser(userA.getId());

        // userB's request must now be CANCELLED with no matchedPartner
        Request updatedReqB = requestRepository.findById(reqB.getId()).orElseThrow();
        assertEquals("CANCELLED", updatedReqB.getStatus(),
                "Partner's request should be CANCELLED when matched user is deleted");
        assertNull(updatedReqB.getMatchedPartner(),
                "Partner reference should be cleared");
    }

    @Test
    void testDeleteUser_AlsoDeletesOwnRequests() {
        User user = userRepository.save(new User("User X", "3333@example.edu", "pass", "STUDENT"));

        Request pending = new Request(user, "TUTOR", testSubject, Set.of("TUE_P2"));
        pending.setStatus("PENDING");
        requestRepository.save(pending);

        userService.deleteUser(user.getId());

        // user's own requests must be gone
        assertFalse(requestRepository.findById(pending.getId()).isPresent(),
                "User's own requests should be deleted");
        assertFalse(userRepository.findById(user.getId()).isPresent(),
                "User should be deleted");
    }
}
