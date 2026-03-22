package com.example.studentportal.service;

import com.example.studentportal.model.Subject;
import com.example.studentportal.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Service: subject catalog access
//
// - retrieve subjects for profile and request forms
// - support data seeder with save and existence check
@Service
@Transactional(readOnly = true) // default read-only TX [overridden for save]
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Autowired
    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getAllSubjects() { return subjectRepository.findAll(); }

    public Optional<Subject> findById(Long id) { return subjectRepository.findById(id); }

    public Optional<Subject> findByCode(String code) { return subjectRepository.findByCode(code); }

    @Transactional
    public Subject save(Subject subject) { return subjectRepository.save(subject); }

    public boolean hasSubjects() { return subjectRepository.count() > 0; }
}