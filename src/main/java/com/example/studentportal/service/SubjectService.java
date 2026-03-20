package com.example.studentportal.service;

import com.example.studentportal.model.Subject;
import com.example.studentportal.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service – subject retrieval and persistence for seeding and request creation
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>fetch all subjects or by ID/code for controllers</li>
 *   <li>save subjects during data seeding</li>
 *   <li>existence check to prevent duplicate seeding</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Autowired
    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getAllSubjects() { return subjectRepository.findAll(); }

    public Optional<Subject> findById(Long id) { return subjectRepository.findById(id); }

    public Optional<Subject> findByCode(String code) {
        return subjectRepository.findByCode(code);
    }

    @Transactional
    public Subject save(Subject subject) { return subjectRepository.save(subject); }

    // true if at least one subject row exists
    public boolean hasSubjects() { return subjectRepository.count() > 0; }
}