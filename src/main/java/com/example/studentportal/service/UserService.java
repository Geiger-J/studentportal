package com.example.studentportal.service;

import com.example.studentportal.model.Role;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for user management operations.
 * Handles user registration, profile updates, and role determination.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the given details.
     * Determines role based on email pattern and encodes password.
     * 
     * @param fullName the user's full name
     * @param email the user's email (must end with @bromsgrove-school.co.uk)
     * @param rawPassword the user's raw password
     * @return the saved user
     * @throws IllegalArgumentException if email is invalid or already exists
     */
    public User registerUser(String fullName, String email, String rawPassword) {
        // Validate email domain
        if (!email.endsWith("@bromsgrove-school.co.uk")) {
            throw new IllegalArgumentException("Email must end with @bromsgrove-school.co.uk");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Determine role based on email pattern
        Role role = determineRoleFromEmail(email);

        // Create and save user
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(fullName, email, hashedPassword, role);
        
        return userRepository.save(user);
    }

    /**
     * Determines user role based on email pattern.
     * If first character of local part (before @) is a digit -> STUDENT, else ADMIN.
     * 
     * @param email the user's email
     * @return the determined role
     */
    public Role determineRoleFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        
        if (!localPart.isEmpty() && Character.isDigit(localPart.charAt(0))) {
            return Role.STUDENT;
        } else {
            return Role.ADMIN;
        }
    }

    /**
     * Finds a user by email address.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Updates user profile information and recalculates profile completeness.
     * 
     * @param user the user to update
     * @return the updated user
     */
    public User updateProfile(User user) {
        // Update profile completeness based on current state
        user.updateProfileCompleteness();
        return userRepository.save(user);
    }

    /**
     * Checks if a user's profile is complete.
     * Profile is complete when yearGroup is set (9-13), at least one subject
     * is selected, and at least one availability slot is selected.
     * 
     * @param user the user to check
     * @return true if profile is complete, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isProfileComplete(User user) {
        return user.isProfileComplete();
    }

    /**
     * Finds a user by ID.
     * 
     * @param id the user ID
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Saves or updates a user.
     * 
     * @param user the user to save
     * @return the saved user
     */
    public User save(User user) {
        return userRepository.save(user);
    }
}