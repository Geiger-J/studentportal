package com.example.studentportal.service;

import com.example.studentportal.model.Role;
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
        // Given
        String fullName = "John Smith";
        String email = "1234student@bromsgrove-school.co.uk";
        String password = "testpass123";

        // When
        User savedUser = userService.registerUser(fullName, email, password);

        // Then
        assertNotNull(savedUser);
        assertEquals(fullName, savedUser.getFullName());
        assertEquals(email, savedUser.getEmail());
        assertEquals(Role.STUDENT, savedUser.getRole());
        
        // Verify password is hashed, not stored in plain text
        assertNotEquals(password, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void testRegisterUser_WithStaffEmail_AssignsAdminRole() {
        // Given
        String fullName = "Jane Teacher";
        String email = "jane.teacher@bromsgrove-school.co.uk";
        String password = "teacherpass";

        // When
        User savedUser = userService.registerUser(fullName, email, password);

        // Then
        assertNotNull(savedUser);
        assertEquals(fullName, savedUser.getFullName());
        assertEquals(email, savedUser.getEmail());
        assertEquals(Role.ADMIN, savedUser.getRole());
        
        // Verify password is hashed
        assertNotEquals(password, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void testRegisterUser_WithInvalidEmailDomain_ThrowsException() {
        // Given
        String fullName = "Invalid User";
        String email = "user@invalid-domain.com";
        String password = "password";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(fullName, email, password)
        );
        
        assertEquals("Email must end with @bromsgrove-school.co.uk", exception.getMessage());
    }

    @Test
    void testRegisterUser_WithDuplicateEmail_ThrowsException() {
        // Given
        String fullName1 = "First User";
        String fullName2 = "Second User";
        String email = "test@bromsgrove-school.co.uk";
        String password = "password";

        // Register first user
        userService.registerUser(fullName1, email, password);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(fullName2, email, password)
        );
        
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void testDetermineRoleFromEmail_StudentEmails() {
        // Test various student email patterns (starting with digits)
        assertEquals(Role.STUDENT, userService.determineRoleFromEmail("1234@bromsgrove-school.co.uk"));
        assertEquals(Role.STUDENT, userService.determineRoleFromEmail("0123student@bromsgrove-school.co.uk"));
        assertEquals(Role.STUDENT, userService.determineRoleFromEmail("9test@bromsgrove-school.co.uk"));
    }

    @Test
    void testDetermineRoleFromEmail_AdminEmails() {
        // Test various admin email patterns (starting with letters)
        assertEquals(Role.ADMIN, userService.determineRoleFromEmail("teacher@bromsgrove-school.co.uk"));
        assertEquals(Role.ADMIN, userService.determineRoleFromEmail("admin@bromsgrove-school.co.uk"));
        assertEquals(Role.ADMIN, userService.determineRoleFromEmail("john.smith@bromsgrove-school.co.uk"));
    }

    @Test
    void testIsProfileComplete_IncompleteProfile() {
        // Given
        User user = new User("Test User", "test@bromsgrove-school.co.uk", "hashedpass", Role.STUDENT);
        
        // When & Then
        assertFalse(userService.isProfileComplete(user));
        assertFalse(user.isProfileComplete());
    }
}