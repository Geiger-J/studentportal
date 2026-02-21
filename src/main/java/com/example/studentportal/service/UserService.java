package com.example.studentportal.service;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final RequestRepository requestRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.requestRepository = requestRepository;
    }

    /**
     * Registers a new user with the given details.
     * Determines role based on email pattern and encodes password.
     *
     * @param fullName the user's full name
     * @param email the user's email (must end with @example.edu)
     * @param rawPassword the user's raw password
     * @return the saved user
     * @throws IllegalArgumentException if email is invalid or already exists
     */
    public User registerUser(String fullName, String email, String rawPassword) {
        // Validate email domain
        if (!email.endsWith("@example.edu")) {
            throw new IllegalArgumentException("Email must end with @example.edu");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Determine role based on email pattern
        String role = determineRoleFromEmail(email);

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
     * @return "STUDENT" or "ADMIN"
     */
    public String determineRoleFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));

        if (!localPart.isEmpty() && Character.isDigit(localPart.charAt(0))) {
            return "STUDENT";
        } else {
            return "ADMIN";
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

    /**
     * Retrieves all users in the system.
     *
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves users filtered by year group (null returns all).
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByYearGroup(Integer yearGroup) {
        if (yearGroup == null) {
            return userRepository.findAll();
        }
        return userRepository.findByYearGroup(yearGroup);
    }

    /**
     * Changes the password for a user identified by ID.
     *
     * @param userId the ID of the user whose password should be changed
     * @param newRawPassword the new plain-text password (will be encoded)
     * @throws IllegalArgumentException if user not found or password is blank
     */
    @Transactional
    public void changePassword(Long userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        if (newRawPassword.length() < 4) {
            throw new IllegalArgumentException("New password must be at least 4 characters");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /**
     * Deletes a user and all their associated data.
     * Before deletion:
     * - Any MATCHED requests this user is part of have their partner's request cancelled.
     *   This way the partner sees a CANCELLED status on their dashboard instead of a broken state.
     * - All of the user's own requests are then deleted along with the user.
     *
     * @param id the ID of the user to delete
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // cancel partner requests: any other user whose request is MATCHED to this user
        // gets their request set to CANCELLED so they know the match fell through
        List<Request> partnerMatchedRequests =
            requestRepository.findByMatchedPartnerAndStatus(user, "MATCHED");
        for (Request partnerRequest : partnerMatchedRequests) {
            partnerRequest.setStatus("CANCELLED");
            partnerRequest.setMatchedPartner(null);
            requestRepository.save(partnerRequest);
        }

        // clear any remaining references to this user as a matched partner
        // (e.g. DONE requests that still hold a reference — set to null safely)
        requestRepository.clearMatchedPartnerReferences(user);

        // delete all tutoring requests owned by this user
        requestRepository.deleteByUser(user);

        // finally remove the user (cascades to user_subjects and user_availability)
        userRepository.delete(user);
    }
}
