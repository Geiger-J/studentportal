package com.example.studentportal.repository;

import com.example.studentportal.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Subject entity operations.
 * Provides database access methods for subject management.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    /**
     * Finds a subject by its code.
     * @param code the subject code to search for
     * @return Optional containing the subject if found
     */
    Optional<Subject> findByCode(String code);
    
    /**
     * Checks if a subject exists with the given code.
     * @param code the code to check
     * @return true if subject exists, false otherwise
     */
    boolean existsByCode(String code);
}