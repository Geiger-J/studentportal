package com.example.studentportal.config;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.repository.SubjectRepository;
import com.example.studentportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Demo data seeder that runs on startup when the "demo" profile is active.
 * Wipes all users and requests, then re-seeds a fixed dataset for recording.
 * Idempotent: every restart produces the same clean state.
 */
@Component
@Profile("demo")
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DemoDataSeeder(UserRepository userRepository,
                          RequestRepository requestRepository,
                          SubjectRepository subjectRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.subjectRepository = subjectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedDemoData();
        } catch (Exception e) {
            logger.error("Demo seeder: FAILED — {}", e.getMessage(), e);
            throw e;
        }
    }

    private void seedDemoData() {
        // ── 1. Clear existing data ──────────────────────────────────────────
        logger.info("Demo seeder: Clearing existing requests and users...");
        long requestCountBefore = requestRepository.count();
        long userCountBefore = userRepository.count();
        requestRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        logger.info("Demo seeder: Deleted {} requests and {} users", requestCountBefore, userCountBefore);

        // ── 2. Shared password hash ─────────────────────────────────────────
        String demoHash = passwordEncoder.encode("demo");

        // ── 3. Look up subjects ─────────────────────────────────────────────
        Subject mathematics = requireSubject("MATHEMATICS");
        Subject english     = requireSubject("ENGLISH");
        Subject physics     = requireSubject("PHYSICS");
        Subject chemistry   = requireSubject("CHEMISTRY");
        Subject biology     = requireSubject("BIOLOGY");
        Subject economics   = requireSubject("ECONOMICS");
        Subject politics    = requireSubject("POLITICS");
        Subject french      = requireSubject("FRENCH");

        // ── 4. Create demo users ────────────────────────────────────────────
        logger.info("Demo seeder: Creating {} demo users...", 9);

        // User 1 — Admin
        User admin = new User("Admin User", "admin@example.edu", demoHash, "ADMIN");
        admin.updateProfileCompleteness();
        admin = userRepository.save(admin);

        // User 2 — Alice Chen  (Year 11, IB)
        User alice = new User("Alice Chen", "23001@example.edu", demoHash, "STUDENT");
        alice.setYearGroup(11);
        alice.setExamBoard("IB");
        alice.setSubjects(Set.of(mathematics, english));
        alice.setAvailability(Set.of("MON_P1", "TUE_P3", "WED_P2", "THU_P4"));
        alice.updateProfileCompleteness();
        alice = userRepository.save(alice);

        // User 3 — Ben Okafor  (Year 10, A_LEVELS)
        User ben = new User("Ben Okafor", "23002@example.edu", demoHash, "STUDENT");
        ben.setYearGroup(10);
        ben.setExamBoard("A_LEVELS");
        ben.setSubjects(Set.of(physics, chemistry));
        ben.setAvailability(Set.of("WED_P5", "THU_P2", "FRI_P1", "MON_P3"));
        ben.updateProfileCompleteness();
        ben = userRepository.save(ben);

        // User 4 — Chloe Dunn  (Year 12, IB)
        User chloe = new User("Chloe Dunn", "23003@example.edu", demoHash, "STUDENT");
        chloe.setYearGroup(12);
        chloe.setExamBoard("IB");
        chloe.setSubjects(Set.of(chemistry, biology));
        chloe.setAvailability(Set.of("MON_P1", "FRI_P4", "TUE_P6", "WED_P5"));
        chloe.updateProfileCompleteness();
        chloe = userRepository.save(chloe);

        // User 5 — David Kim  (Year 13, IB)
        User david = new User("David Kim", "23004@example.edu", demoHash, "STUDENT");
        david.setYearGroup(13);
        david.setExamBoard("IB");
        david.setSubjects(Set.of(mathematics, economics));
        david.setAvailability(Set.of("FRI_P6", "FRI_P7", "THU_P1"));
        david.updateProfileCompleteness();
        david = userRepository.save(david);

        // User 6 — Emma Wilson  (Year 11, A_LEVELS)
        User emma = new User("Emma Wilson", "23005@example.edu", demoHash, "STUDENT");
        emma.setYearGroup(11);
        emma.setExamBoard("A_LEVELS");
        emma.setSubjects(Set.of(physics, mathematics));
        emma.setAvailability(Set.of("MON_P3", "TUE_P3", "THU_P5", "WED_P5"));
        emma.updateProfileCompleteness();
        emma = userRepository.save(emma);

        // User 7 — Fatima Al-Rashid  (Year 12, IB)
        User fatima = new User("Fatima Al-Rashid", "23006@example.edu", demoHash, "STUDENT");
        fatima.setYearGroup(12);
        fatima.setExamBoard("IB");
        fatima.setSubjects(Set.of(economics, politics, english));
        fatima.setAvailability(Set.of("MON_P1", "TUE_P2", "WED_P3", "THU_P4", "FRI_P5"));
        fatima.updateProfileCompleteness();
        fatima = userRepository.save(fatima);

        // User 8 — George Papadopoulos  (Year 9, GCSE)
        User george = new User("George Papadopoulos", "23007@example.edu", demoHash, "STUDENT");
        george.setYearGroup(9);
        george.setExamBoard("GCSE");
        george.setSubjects(Set.of(mathematics, english, french));
        george.setAvailability(Set.of("MON_P1", "TUE_P1", "WED_P1", "THU_P1"));
        george.updateProfileCompleteness();
        george = userRepository.save(george);

        // User 9 — Hannah Liu  (incomplete profile — SC 4 demo)
        User hannah = new User("Hannah Liu", "23008@example.edu", demoHash, "STUDENT");
        // No yearGroup, examBoard, subjects or availability set — profileComplete stays false
        hannah.updateProfileCompleteness();
        hannah = userRepository.save(hannah);

        // ── 5. Create demo requests ─────────────────────────────────────────
        logger.info("Demo seeder: Creating {} demo requests...", 14);

        List<Request> requests = List.of(
            // Alice — TUTEE Mathematics (MON_P1, TUE_P3)
            new Request(alice, "TUTEE", mathematics, Set.of("MON_P1", "TUE_P3")),
            // Alice — TUTEE English (TUE_P3, WED_P2)
            new Request(alice, "TUTEE", english, Set.of("TUE_P3", "WED_P2")),
            // Ben — TUTEE Physics (WED_P5, THU_P2)
            new Request(ben, "TUTEE", physics, Set.of("WED_P5", "THU_P2")),
            // Ben — TUTEE Chemistry (FRI_P1, MON_P3)
            new Request(ben, "TUTEE", chemistry, Set.of("FRI_P1", "MON_P3")),
            // Chloe — TUTOR Chemistry (MON_P1, FRI_P4, WED_P5)
            new Request(chloe, "TUTOR", chemistry, Set.of("MON_P1", "FRI_P4", "WED_P5")),
            // Chloe — TUTOR Biology (TUE_P6, WED_P5)
            new Request(chloe, "TUTOR", biology, Set.of("TUE_P6", "WED_P5")),
            // David — TUTEE Mathematics (FRI_P6)
            new Request(david, "TUTEE", mathematics, Set.of("FRI_P6")),
            // David — TUTOR Economics (FRI_P7, THU_P1)
            new Request(david, "TUTOR", economics, Set.of("FRI_P7", "THU_P1")),
            // Emma — TUTEE Physics (MON_P3, TUE_P3, WED_P5)
            new Request(emma, "TUTEE", physics, Set.of("MON_P3", "TUE_P3", "WED_P5")),
            // Emma — TUTEE Mathematics (THU_P5)
            new Request(emma, "TUTEE", mathematics, Set.of("THU_P5")),
            // Fatima — TUTOR Economics (MON_P1, WED_P3, FRI_P5)
            new Request(fatima, "TUTOR", economics, Set.of("MON_P1", "WED_P3", "FRI_P5")),
            // Fatima — TUTEE English (TUE_P2, THU_P4)
            new Request(fatima, "TUTEE", english, Set.of("TUE_P2", "THU_P4")),
            // George — TUTEE Mathematics (MON_P1, TUE_P1)
            new Request(george, "TUTEE", mathematics, Set.of("MON_P1", "TUE_P1")),
            // George — TUTEE French (WED_P1, THU_P1)
            new Request(george, "TUTEE", french, Set.of("WED_P1", "THU_P1"))
        );

        requestRepository.saveAll(requests);

        // ── 6. Summary ──────────────────────────────────────────────────────
        logger.info("Demo seeder: Setup complete — {} users, {} requests ready for recording",
                userRepository.count(), requestRepository.count());
    }

    /**
     * Looks up a Subject by code, throwing a descriptive exception if not found.
     * DataSeeder (Order 1) must have run first to populate subjects.
     */
    private Subject requireSubject(String code) {
        return subjectRepository.findByCode(code).orElseThrow(() -> {
            logger.error("Demo seeder: Subject with code '{}' not found — check DataSeeder", code);
            return new IllegalStateException(
                    "Demo seeder: Subject with code '" + code + "' not found — check DataSeeder");
        });
    }
}
