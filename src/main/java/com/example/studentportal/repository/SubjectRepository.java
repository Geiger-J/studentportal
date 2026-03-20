package com.example.studentportal.repository;

import com.example.studentportal.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository – JPA repository for Subject entities
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>lookup by code for seeding and request validation</li>
 *   <li>existence check to prevent duplicate seeding</li>
 * </ul>
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);
}