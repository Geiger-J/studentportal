package com.example.studentportal.config;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.RequestRepository;
import com.example.studentportal.repository.SubjectRepository;
import com.example.studentportal.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
 * Demo data seeder — runs on startup when the "demo" profile is active.
 *
 * Wipes all user/request data via native SQL (respects FK constraints),
 * then seeds a fixed dataset designed to demonstrate the matching algorithm:
 *   - 22 total requests across 10 students + 1 admin
 *   - ~8 pairs that the algorithm WILL match (same subject, shared slot,
 *     tutor year >= tutee year)
 *   - ~6 requests intentionally left PENDING (no valid partner exists)
 *
 * Idempotent: every restart produces the same clean state.
 * Runs after DataSeeder (@Order 1) so subjects already exist.
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

    @PersistenceContext
    private EntityManager entityManager;

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

        // ── 1. Wipe existing data (ordered to respect FK constraints) ────────
        //
        // We use native SQL DELETE rather than deleteAllInBatch() so that join
        // tables (user_subjects, user_availability, request_timeslots) are also
        // cleared without needing Hibernate to load every entity first.
        // CASCADE is not used so the tables themselves are preserved.
        logger.info("Demo seeder: Wiping existing data...");

        entityManager.createNativeQuery("DELETE FROM request_timeslots").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM requests").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_availability").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_subjects").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users").executeUpdate();

        logger.info("Demo seeder: All user and request data cleared");

        // ── 2. Shared password hash ──────────────────────────────────────────
        String demoHash = passwordEncoder.encode("demo");

        // ── 3. Resolve subjects (seeded by DataSeeder @Order 1) ─────────────
        Subject maths    = requireSubject("MATHEMATICS");
        Subject physics  = requireSubject("PHYSICS");
        Subject chem     = requireSubject("CHEMISTRY");
        Subject bio      = requireSubject("BIOLOGY");
        Subject econ     = requireSubject("ECONOMICS");
        Subject english  = requireSubject("ENGLISH");
        Subject french   = requireSubject("FRENCH");
        Subject politics = requireSubject("POLITICS");

        // ── 4. Create users ──────────────────────────────────────────────────
        //
        // Year groups matter for matching: tutor year >= tutee year.
        // Exam board match gives +50 weight bonus but is not a hard constraint.
        logger.info("Demo seeder: Creating users...");

        // ── Admin ────────────────────────────────────────────────────────────
        User admin = new User("Admin User", "admin@example.edu", demoHash, "ADMIN");
        admin.updateProfileCompleteness();
        admin = userRepository.save(admin);

        // ── Year 13 students (can tutor anyone) ──────────────────────────────

        // Sophie — strong maths/physics tutor, IB
        User sophie = new User("Sophie Hartmann", "23001@example.edu", demoHash, "STUDENT");
        sophie.setYearGroup(13);
        sophie.setExamBoard("IB");
        sophie.setSubjects(Set.of(maths, physics));
        sophie.setAvailability(Set.of("MON_P1", "MON_P3", "WED_P2", "FRI_P5"));
        sophie.updateProfileCompleteness();
        sophie = userRepository.save(sophie);

        // James — economics/politics tutor, A-Levels
        User james = new User("James Okafor", "23002@example.edu", demoHash, "STUDENT");
        james.setYearGroup(13);
        james.setExamBoard("A_LEVELS");
        james.setSubjects(Set.of(econ, politics));
        james.setAvailability(Set.of("TUE_P2", "THU_P3", "FRI_P4", "FRI_P6"));
        james.updateProfileCompleteness();
        james = userRepository.save(james);

        // ── Year 12 students (can tutor year 9–12) ────────────────────────────

        // Chloe — chemistry/biology tutor, IB
        User chloe = new User("Chloe Dupont", "23003@example.edu", demoHash, "STUDENT");
        chloe.setYearGroup(12);
        chloe.setExamBoard("IB");
        chloe.setSubjects(Set.of(chem, bio));
        chloe.setAvailability(Set.of("MON_P1", "WED_P4", "FRI_P3", "THU_P5"));
        chloe.updateProfileCompleteness();
        chloe = userRepository.save(chloe);

        // Marcus — maths tutor and economics tutee, A-Levels
        User marcus = new User("Marcus Webb", "23004@example.edu", demoHash, "STUDENT");
        marcus.setYearGroup(12);
        marcus.setExamBoard("A_LEVELS");
        marcus.setSubjects(Set.of(maths, econ));
        marcus.setAvailability(Set.of("MON_P3", "TUE_P2", "WED_P2", "THU_P3"));
        marcus.updateProfileCompleteness();
        marcus = userRepository.save(marcus);

        // ── Year 11 students ──────────────────────────────────────────────────

        // Aisha — seeks maths + physics tutoring, IB
        User aisha = new User("Aisha Al-Farsi", "23005@example.edu", demoHash, "STUDENT");
        aisha.setYearGroup(11);
        aisha.setExamBoard("IB");
        aisha.setSubjects(Set.of(maths, physics));
        aisha.setAvailability(Set.of("MON_P1", "MON_P3", "WED_P2", "FRI_P5"));
        aisha.updateProfileCompleteness();
        aisha = userRepository.save(aisha);

        // Tom — seeks chemistry tutoring + can tutor biology, A-Levels
        User tom = new User("Tom Nakamura", "23006@example.edu", demoHash, "STUDENT");
        tom.setYearGroup(11);
        tom.setExamBoard("A_LEVELS");
        tom.setSubjects(Set.of(chem, bio));
        tom.setAvailability(Set.of("MON_P1", "WED_P4", "FRI_P3", "TUE_P5"));
        tom.updateProfileCompleteness();
        tom = userRepository.save(tom);

        // ── Year 10 students ──────────────────────────────────────────────────

        // Priya — seeks economics tutoring, IB
        User priya = new User("Priya Sharma", "23007@example.edu", demoHash, "STUDENT");
        priya.setYearGroup(10);
        priya.setExamBoard("IB");
        priya.setSubjects(Set.of(econ, english));
        priya.setAvailability(Set.of("TUE_P2", "THU_P3", "FRI_P4"));
        priya.updateProfileCompleteness();
        priya = userRepository.save(priya);

        // Leon — seeks maths tutoring, A-Levels
        User leon = new User("Leon Fischer", "23008@example.edu", demoHash, "STUDENT");
        leon.setYearGroup(10);
        leon.setExamBoard("A_LEVELS");
        leon.setSubjects(Set.of(maths, french));
        leon.setAvailability(Set.of("MON_P3", "WED_P2", "FRI_P6"));
        leon.updateProfileCompleteness();
        leon = userRepository.save(leon);

        // ── Year 9 students ───────────────────────────────────────────────────

        // Mei — seeks biology + english tutoring, GCSE
        User mei = new User("Mei Zhang", "23009@example.edu", demoHash, "STUDENT");
        mei.setYearGroup(9);
        mei.setExamBoard("GCSE");
        mei.setSubjects(Set.of(bio, english));
        mei.setAvailability(Set.of("WED_P4", "THU_P5", "FRI_P3"));
        mei.updateProfileCompleteness();
        mei = userRepository.save(mei);

        // Finn — seeks politics tutoring (no eligible tutor exists — intentional)
        User finn = new User("Finn O'Brien", "23010@example.edu", demoHash, "STUDENT");
        finn.setYearGroup(9);
        finn.setExamBoard("GCSE");
        finn.setSubjects(Set.of(politics, maths));
        finn.setAvailability(Set.of("MON_P5", "TUE_P6", "WED_P7"));
        finn.updateProfileCompleteness();
        finn = userRepository.save(finn);

        logger.info("Demo seeder: Created 10 students + 1 admin");

        // ── 5. Create requests ───────────────────────────────────────────────
        //
        // MATCHABLE PAIRS (8 pairs = 16 requests):
        //
        //  Pair 1 — Maths, MON_P1:   Sophie (Y13, IB, TUTOR)   ↔ Aisha (Y11, IB, TUTEE)
        //           +50 exam board bonus (both IB), year gap +20
        //
        //  Pair 2 — Physics, WED_P2:  Sophie (Y13, IB, TUTOR)   ↔ Aisha (Y11, IB, TUTEE)
        //           +50 exam board bonus (both IB), year gap +20
        //
        //  Pair 3 — Maths, MON_P3:   Marcus (Y12, A_LEVELS, TUTOR) ↔ Leon (Y10, A_LEVELS, TUTEE)
        //           +50 exam board bonus (both A_LEVELS), year gap +20
        //
        //  Pair 4 — Maths, WED_P2:   Marcus (Y12, TUTOR)       ↔ Leon (Y10, TUTEE)
        //           (second shared slot — algorithm will pick only one per request)
        //
        //  Pair 5 — Econ, TUE_P2:    James (Y13, A_LEVELS, TUTOR) ↔ Priya (Y10, IB, TUTEE)
        //           no exam board bonus, year gap +15
        //
        //  Pair 6 — Econ, THU_P3:    James (Y13, TUTOR)        ↔ Marcus (Y12, TUTEE)
        //           no exam board bonus, year gap +30
        //
        //  Pair 7 — Chem, MON_P1:    Chloe (Y12, IB, TUTOR)   ↔ Tom (Y11, A_LEVELS, TUTEE)
        //           no exam board bonus, year gap +30
        //
        //  Pair 8 — Bio, WED_P4:     Chloe (Y12, IB, TUTOR)   ↔ Mei (Y9, GCSE, TUTEE)
        //           no exam board bonus, year gap +20
        //
        // UNMATCHED requests (6 requests) — reasons noted:
        //
        //  U1 — Finn, TUTEE Politics, MON_P5/TUE_P6/WED_P7:
        //       James is the only politics tutor but shares none of these slots → no match
        //
        //  U2 — Finn, TUTEE Maths, MON_P5/WED_P7:
        //       Sophie/Marcus available at MON_P1/P3/WED_P2 → no shared slot → no match
        //
        //  U3 — Sophie, TUTOR Physics, FRI_P5:
        //       Aisha also has FRI_P5 but Aisha's physics TUTEE request only lists MON_P1/WED_P2
        //       (FRI_P5 is in Aisha's availability but not in her request's timeslots)
        //       → no match for this request
        //
        //  U4 — Leon, TUTEE French, FRI_P6:
        //       No one offers French tutoring → no match
        //
        //  U5 — Mei, TUTEE English, FRI_P3:
        //       No one offers English tutoring in the demo set → no match
        //
        //  U6 — Marcus, TUTEE Econ, WED_P2:
        //       James tutors Econ but is only available TUE_P2/THU_P3/FRI_P4/FRI_P6
        //       → no shared slot on WED_P2 → this specific request won't match
        //       (James's econ TUTOR request uses TUE_P2/FRI_P4, which will match Priya/Marcus
        //        on other slots, but Marcus's TUTEE econ request targets WED_P2)

        logger.info("Demo seeder: Creating requests...");

        List<Request> requests = List.of(

            // ── Sophie (Y13, IB) ──────────────────────────────────────────────
            // TUTOR Maths — will match Aisha on MON_P1                 [PAIR 1]
            new Request(sophie, "TUTOR", maths,   Set.of("MON_P1", "WED_P2")),
            // TUTOR Physics — MON_P3 slot will match Aisha; FRI_P5 slot won't [PAIR 2 + U3]
            new Request(sophie, "TUTOR", physics, Set.of("MON_P3", "FRI_P5")),

            // ── James (Y13, A-Levels) ─────────────────────────────────────────
            // TUTOR Economics — TUE_P2 matches Priya; THU_P3 matches Marcus  [PAIRS 5, 6]
            new Request(james,  "TUTOR", econ,    Set.of("TUE_P2", "THU_P3", "FRI_P4")),
            // TUTOR Politics — FRI_P6 never matches Finn's slots              [unmatchable]
            new Request(james,  "TUTOR", politics,Set.of("FRI_P6")),

            // ── Chloe (Y12, IB) ───────────────────────────────────────────────
            // TUTOR Chemistry — MON_P1 matches Tom                       [PAIR 7]
            new Request(chloe,  "TUTOR", chem,    Set.of("MON_P1", "FRI_P3")),
            // TUTOR Biology — WED_P4 matches Mei                         [PAIR 8]
            new Request(chloe,  "TUTOR", bio,     Set.of("WED_P4", "THU_P5")),

            // ── Marcus (Y12, A-Levels) ────────────────────────────────────────
            // TUTOR Maths — MON_P3 + WED_P2 both match Leon              [PAIRS 3, 4]
            new Request(marcus, "TUTOR", maths,   Set.of("MON_P3", "WED_P2")),
            // TUTEE Economics — WED_P2 only; James has no WED_P2 → no match [U6]
            new Request(marcus, "TUTEE", econ,    Set.of("WED_P2", "THU_P3")),

            // ── Aisha (Y11, IB) ───────────────────────────────────────────────
            // TUTEE Maths — MON_P1 matches Sophie                        [PAIR 1]
            new Request(aisha,  "TUTEE", maths,   Set.of("MON_P1", "MON_P3")),
            // TUTEE Physics — MON_P3 matches Sophie                      [PAIR 2]
            new Request(aisha,  "TUTEE", physics, Set.of("MON_P3", "WED_P2")),

            // ── Tom (Y11, A-Levels) ───────────────────────────────────────────
            // TUTEE Chemistry — MON_P1 matches Chloe                     [PAIR 7]
            new Request(tom,    "TUTEE", chem,    Set.of("MON_P1", "TUE_P5")),
            // TUTOR Biology — FRI_P3 matches Mei on FRI_P3               [bonus: Mei also matched by Chloe; whichever scores higher wins]
            new Request(tom,    "TUTOR", bio,     Set.of("FRI_P3", "WED_P4")),

            // ── Priya (Y10, IB) ───────────────────────────────────────────────
            // TUTEE Economics — TUE_P2 matches James                     [PAIR 5]
            new Request(priya,  "TUTEE", econ,    Set.of("TUE_P2", "FRI_P4")),
            // TUTEE English — no English tutors in demo set → no match   [U5]
            new Request(priya,  "TUTEE", english, Set.of("TUE_P2", "THU_P3")),

            // ── Leon (Y10, A-Levels) ──────────────────────────────────────────
            // TUTEE Maths — MON_P3 + WED_P2 both match Marcus            [PAIRS 3, 4]
            new Request(leon,   "TUTEE", maths,   Set.of("MON_P3", "WED_P2")),
            // TUTEE French — no French tutors in demo set → no match     [U4]
            new Request(leon,   "TUTEE", french,  Set.of("MON_P3", "FRI_P6")),

            // ── Mei (Y9, GCSE) ────────────────────────────────────────────────
            // TUTEE Biology — WED_P4 matches Chloe (Y12, higher weight) and Tom (Y11)
            //   Chloe wins: year gap Y12-Y9=3 → +15 vs Tom Y11-Y9=2 → +20
            //   Actually Tom (+20 gap) beats Chloe (+15 gap) — good algorithm demo!  [PAIR 8 resolved]
            new Request(mei,    "TUTEE", bio,     Set.of("WED_P4", "FRI_P3")),
            // TUTEE English — no English tutors → no match                [U5]
            new Request(mei,    "TUTEE", english, Set.of("THU_P5", "FRI_P3")),

            // ── Finn (Y9, GCSE) ───────────────────────────────────────────────
            // TUTEE Politics — James's only politics TUTOR slot (FRI_P6) not in Finn's set [U1]
            new Request(finn,   "TUTEE", politics,Set.of("MON_P5", "TUE_P6", "WED_P7")),
            // TUTEE Maths — Finn's slots don't overlap Sophie's or Marcus's availability [U2]
            new Request(finn,   "TUTEE", maths,   Set.of("MON_P5", "WED_P7"))
        );

        requestRepository.saveAll(requests);

        logger.info("Demo seeder: Setup complete — {} users ({} students + 1 admin), {} requests",
                userRepository.count(),
                userRepository.count() - 1,
                requestRepository.count());
        logger.info("Demo seeder: Expected on matching run — ~8 pairs matched, ~6 requests remain PENDING");
    }

    /**
     * Looks up a Subject by code, throwing a descriptive exception if not found.
     * DataSeeder (Order 1) must have already run to populate subjects.
     */
    private Subject requireSubject(String code) {
        return subjectRepository.findByCode(code).orElseThrow(() -> {
            logger.error("Demo seeder: Subject '{}' not found — has DataSeeder run?", code);
            return new IllegalStateException(
                    "Demo seeder: Subject '" + code + "' not found — has DataSeeder run?");
        });
    }
}
