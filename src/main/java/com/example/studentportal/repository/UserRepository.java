package com.example.studentportal.repository;

import com.example.studentportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository: JPA repository for User entities
//
// Responsibilities:
// - look up users by email and year group
// - check existence by email for duplicate detection
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // find single user by email - used for login and profile lookup
    Optional<User> findByEmail(String email);

    // existence check - used during registration to prevent duplicates
    boolean existsByEmail(String email);

    List<User> findByYearGroup(Integer yearGroup);
}