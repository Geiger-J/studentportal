package com.example.studentportal.repository;

import com.example.studentportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository – JPA repository for User entities
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>lookup and existence check by email for authentication and registration</li>
 *   <li>filter users by year group for admin views</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByYearGroup(Integer yearGroup);
}