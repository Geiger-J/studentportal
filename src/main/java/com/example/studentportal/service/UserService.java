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

/*
 * Service – user registration, profile management, and deletion
 *
 * Responsibilities:
 * - validate email domain and uniqueness; determine role from email prefix
 * - encode password with BCrypt; persist user
 * - cascade-cancel matched partner requests before deleting a user
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RequestRepository requestRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.requestRepository = requestRepository;
    }

    // validate email → determine role → encode password → persist
    public User registerUser(String fullName, String email, String rawPassword) {
        if (!email.endsWith("@example.edu")) {
            throw new IllegalArgumentException("Email must end with @example.edu");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String role = determineRoleFromEmail(email);

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(fullName, email, hashedPassword, role);

        return userRepository.save(user);
    }

    // digit-first local part → STUDENT, else ADMIN [e.g., 12345@example.edu → STUDENT]
    public String determineRoleFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));

        if (!localPart.isEmpty() && Character.isDigit(localPart.charAt(0))) {
            return "STUDENT";
        } else {
            return "ADMIN";
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }

    public User updateProfile(User user) {
        // Update profile completeness based on current state
        user.updateProfileCompleteness();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isProfileComplete(User user) { return user.isProfileComplete(); }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) { return userRepository.findById(id); }

    public User save(User user) { return userRepository.save(user); }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() { return userRepository.findAll(); }

    // null yearGroup returns all users
    @Transactional(readOnly = true)
    public List<User> getUsersByYearGroup(Integer yearGroup) {
        if (yearGroup == null) {
            return userRepository.findAll();
        }
        return userRepository.findByYearGroup(yearGroup);
    }

    // validate length → encode → save [min 4 chars]
    @Transactional
    public void changePassword(Long userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        if (newRawPassword.length() < 4) {
            throw new IllegalArgumentException("New password must be at least 4 characters");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User not found with id: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    // cancel partner MATCHED requests → clear partner refs → delete user's requests → delete user
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // cancel partner's MATCHED requests [notify them match fell through]
        List<Request> partnerMatchedRequests = requestRepository.findByMatchedPartnerAndStatus(user,
                "MATCHED");
        for (Request partnerRequest : partnerMatchedRequests) {
            partnerRequest.setStatus("CANCELLED");
            partnerRequest.setMatchedPartner(null);
            requestRepository.save(partnerRequest);
        }

        // clear remaining partner refs [e.g., DONE requests]
        requestRepository.clearMatchedPartnerReferences(user);

        // delete all tutoring requests owned by this user
        requestRepository.deleteByUser(user);

        // finally remove the user (cascades to user_subjects and user_availability)
        userRepository.delete(user);
    }
}
