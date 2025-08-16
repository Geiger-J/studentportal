package com.example.studentportal.service;

import com.example.studentportal.model.Subject;
import com.example.studentportal.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for subject management operations.
 * Handles subject retrieval and management.
 */
@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Autowired
    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    /**
     * Retrieves all available subjects.
     * 
     * @return list of all subjects
     */
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    /**
     * Finds a subject by its ID.
     * 
     * @param id the subject ID
     * @return Optional containing the subject if found
     */
    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    /**
     * Finds a subject by its code.
     * 
     * @param code the subject code
     * @return Optional containing the subject if found
     */
    public Optional<Subject> findByCode(String code) {
        return subjectRepository.findByCode(code);
    }

    /**
     * Saves a subject (for seeding purposes).
     * 
     * @param subject the subject to save
     * @return the saved subject
     */
    @Transactional
    public Subject save(Subject subject) {
        return subjectRepository.save(subject);
    }

    /**
     * Checks if any subjects exist in the database.
     * 
     * @return true if subjects exist, false if database is empty
     */
    public boolean hasSubjects() {
        return subjectRepository.count() > 0;
    }
}