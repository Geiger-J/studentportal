package com.example.studentportal.config;

import com.example.studentportal.model.Subject;
import com.example.studentportal.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Configuration: seeds initial subject data on startup
//
// Responsibilities:
// - insert default subjects if table is empty
// - run once per startup (skipped if data already exists)

// run seeder only if property set [defaults to true if absent]
@Component
@Order(1)
@ConditionalOnProperty(name = "app.data-seeder.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final SubjectService subjectService;

    @Autowired
    public DataSeeder(SubjectService subjectService) { this.subjectService = subjectService; }

    @Override
    public void run(String... args) throws Exception { seedSubjects(); }

    // skip if subjects already exist; prevents re-seeding on restart
    private void seedSubjects() {
        if (subjectService.hasSubjects()) {
            logger.info("Subjects already exist, skipping seeding");
            return;
        }

        logger.info("Seeding subjects...");

        // curated list shared across all exam boards: [code, displayName]
        String[][] subjectData = {
                // Languages
                { "ENGLISH", "English" }, { "GERMAN", "German" }, { "FRENCH", "French" },

                // STEM
                { "MATHEMATICS", "Mathematics" }, { "PHYSICS", "Physics" },
                { "BIOLOGY", "Biology" }, { "CHEMISTRY", "Chemistry" },

                // Social Sciences
                { "ECONOMICS", "Economics" }, { "POLITICS", "Politics" },
                { "BUSINESS", "Business" } };

        for (String[] subjectInfo : subjectData) {
            Subject subject = new Subject(subjectInfo[0], subjectInfo[1]);
            subjectService.save(subject);
            logger.debug("Seeded subject: {} - {}", subjectInfo[0], subjectInfo[1]);
        }

        logger.info("Successfully seeded {} subjects", subjectData.length);
    }
}