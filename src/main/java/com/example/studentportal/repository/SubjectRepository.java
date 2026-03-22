package com.example.studentportal.repository;

import com.example.studentportal.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repository: JPA repository for Subject entities
//
// - look up subjects by code
// - check existence by code for seeder deduplication
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // find subject by its natural key
    Optional<Subject> findByCode(String code);

    // existence check used by seeder to avoid re-inserting
    boolean existsByCode(String code);
}