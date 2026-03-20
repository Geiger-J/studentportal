package com.example.studentportal.config;

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
 * Configuration – seeds initial subjects on startup if none exist
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>runs once via CommandLineRunner on startup</li>
 *   <li>skips seeding if subjects already present</li>
 *   <li>creates curated set of Language, STEM, and Social Science subjects</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.data-seeder.enabled", havingValue = "true", matchIfMissing = true) // only runs if seeder enabled [defaults to true if property absent]
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final SubjectService subjectService;

    @Autowired
    public DataSeeder(SubjectService subjectService) { this.subjectService = subjectService; }

    @Override
    public void run(String... args) throws Exception { seedSubjects(); }

    // seed subjects → skip if already populated
    private void seedSubjects() {
        if (subjectService.hasSubjects()) {
            logger.info("Subjects already exist, skipping seeding");
            return;
        }

        logger.info("Seeding subjects...");

        // curated subject list shared across all exam boards
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