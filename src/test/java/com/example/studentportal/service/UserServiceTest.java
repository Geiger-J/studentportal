package com.example.studentportal.service;

import com.example.studentportal.model.User;
import com.example.studentportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
}
