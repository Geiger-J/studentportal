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

// Service: user account management
//
// Responsibilities:
// - register new users, deriving role from email prefix
// - update profile and cache completeness flag
// - delete users with cascade cleanup of requests and matches
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

    // validate email domain -> derive role -> encode password -> persist
    public User registerUser(String fullName, String email, String rawPassword) {
        if (!email.endsWith("@example.edu")) {
            throw new IllegalArgumentException("Email must end with @example.edu");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // derive role then encode password
        String role = determineRoleFromEmail(email);

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(fullName, email, hashedPassword, role);

        return userRepository.save(user);
    }

    // local part starts with digit -> STUDENT, else ADMIN
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

    // recalculate completeness flag then persist
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

    @Transactional(readOnly = true)
    public List<User> getUsersByYearGroup(Integer yearGroup) {
        if (yearGroup == null) {
            return userRepository.findAll();
        }
        return userRepository.findByYearGroup(yearGroup);
    }

    // encode and persist new password; rejects blank or too-short passwords
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

    // cancel partner matches -> clear remaining partner refs -> delete user's
    // requests -> delete user
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // cancel partner requests: partner sees CANCELLED so they know the match fell
        // through
        List<Request> partnerMatchedRequests = requestRepository.findByMatchedPartnerAndStatus(user,
                "MATCHED");
        for (Request partnerRequest : partnerMatchedRequests) {
            partnerRequest.setStatus("CANCELLED");
            partnerRequest.setMatchedPartner(null);
            requestRepository.save(partnerRequest);
        }

        // null out remaining partner references [e.g. DONE requests still holding a
        // ref]
        requestRepository.clearMatchedPartnerReferences(user);

        requestRepository.deleteByUser(user);

        // delete user; cascades to user_subjects and user_availability join tables
        userRepository.delete(user);
    }
}
