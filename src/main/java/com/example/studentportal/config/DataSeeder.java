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

        // Define standard subjects with exam boards
        // Years 9-11: GCSE subjects
        // Years 12-13: A_LEVELS and IB subjects
        // Some subjects are available in multiple exam boards (need multiple entries)
        
        String[][] subjectData = {
            // GCSE subjects (Years 9-11)
            {"GCSE_ENGLISH", "English", "GCSE"},
            {"GCSE_GERMAN", "German", "GCSE"},
            {"GCSE_FRENCH", "French", "GCSE"},
            {"GCSE_MATHEMATICS", "Mathematics", "GCSE"},
            {"GCSE_PHYSICS", "Physics", "GCSE"},
            {"GCSE_BIOLOGY", "Biology", "GCSE"},
            {"GCSE_CHEMISTRY", "Chemistry", "GCSE"},
            {"GCSE_ECONOMICS", "Economics", "GCSE"},
            {"GCSE_POLITICS", "Politics", "GCSE"},
            {"GCSE_BUSINESS", "Business", "GCSE"},
            
            // A-Level subjects (Years 12-13)
            {"AL_ENGLISH", "English", "A_LEVELS"},
            {"AL_GERMAN", "German", "A_LEVELS"},
            {"AL_FRENCH", "French", "A_LEVELS"},
            {"AL_MATHEMATICS", "Mathematics", "A_LEVELS"},
            {"AL_PHYSICS", "Physics", "A_LEVELS"},
            {"AL_BIOLOGY", "Biology", "A_LEVELS"},
            {"AL_CHEMISTRY", "Chemistry", "A_LEVELS"},
            {"AL_ECONOMICS", "Economics", "A_LEVELS"},
            {"AL_POLITICS", "Politics", "A_LEVELS"},
            {"AL_BUSINESS", "Business", "A_LEVELS"},
            
            // IB subjects (Years 12-13)
            {"IB_ENGLISH", "English", "IB"},
            {"IB_GERMAN", "German", "IB"},
            {"IB_FRENCH", "French", "IB"},
            {"IB_MATHS_AA", "Mathematics AA", "IB"},
            {"IB_MATHS_AI", "Mathematics AI", "IB"},
            {"IB_PHYSICS", "Physics", "IB"},
            {"IB_BIOLOGY", "Biology", "IB"},
            {"IB_CHEMISTRY", "Chemistry", "IB"},
            {"IB_ECONOMICS", "Economics", "IB"},
            {"IB_POLITICS", "Politics", "IB"},
            {"IB_BUSINESS", "Business Management", "IB"}
        };

        for (String[] subjectInfo : subjectData) {
            ExamBoard examBoard = ExamBoard.valueOf(subjectInfo[2]);
            Subject subject = new Subject(subjectInfo[0], subjectInfo[1], examBoard);
            subjectService.save(subject);
            logger.debug("Seeded subject: {} - {} ({})", subjectInfo[0], subjectInfo[1], subjectInfo[2]);
        }

        logger.info("Successfully seeded {} subjects", subjectData.length);
    }
}