package com.example.studentportal.config;

import com.example.studentportal.model.ExamBoard;
import com.example.studentportal.model.Subject;
import com.example.studentportal.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Data seeding component that runs on application startup.
 * Seeds the database with initial subjects if none exist.
 * 
 * Note: Using simple CommandLineRunner for early iteration. 
 * In production, this should be replaced with Flyway migrations
 * for better version control and deployment consistency.
 */
@Component
@ConditionalOnProperty(name = "app.data-seeder.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final SubjectService subjectService;

    @Autowired
    public DataSeeder(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Override
    public void run(String... args) throws Exception {
        seedSubjects();
    }

    /**
     * Seeds the database with standard subjects if none exist.
     * Prevents duplicate seeding on application restarts.
     */
    private void seedSubjects() {
        if (subjectService.hasSubjects()) {
            logger.info("Subjects already exist, skipping seeding");
            return;
        }

        logger.info("Seeding subjects...");

        // Define standard subjects - simple curated list shared across all exam boards
        String[][] subjectData = {
            // Languages
            {"ENGLISH", "English"},
            {"GERMAN", "German"},
            {"FRENCH", "French"},
            
            // STEM
            {"MATHEMATICS", "Mathematics"},
            {"PHYSICS", "Physics"},
            {"BIOLOGY", "Biology"},
            {"CHEMISTRY", "Chemistry"},
            
            // Social Sciences
            {"ECONOMICS", "Economics"},
            {"POLITICS", "Politics"},
            {"BUSINESS", "Business"}
        };

        for (String[] subjectInfo : subjectData) {
            Subject subject = new Subject(subjectInfo[0], subjectInfo[1]);
            subjectService.save(subject);
            logger.debug("Seeded subject: {} - {}", subjectInfo[0], subjectInfo[1]);
        }

        logger.info("Successfully seeded {} subjects", subjectData.length);
    }
}