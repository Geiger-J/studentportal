# Criterion C: Development

## Introduction

The student peer-tutoring portal automates matching of students who offer tutoring with those who seek it, removing time-consuming manual coordination by school staff. Development was guided by twenty verifiable success criteria covering weighted matching, role-based access control, scheduled status transitions, and transactional data integrity. This section explains how each major decision meets those criteria, with reference to the specific files and methods where each technique is implemented.

---

## Section 1: Product Structure

The application follows the **Spring MVC layered architecture**: controllers translate HTTP requests into Java method calls, services contain all business logic and transaction boundaries, repositories expose database queries via Spring Data JPA, and a relational database provides persistence. A cross-cutting configuration layer handles security rules, bean definitions, and start-up concerns.

| Package | Responsibility |
|---------|----------------|
| `controller` | Translate HTTP requests; delegate to services |
| `service` | Business logic and transaction boundaries |
| `repository` | Database queries via Spring Data JPA |
| `model` | JPA entities: `User`, `Request`, `Subject` |
| `config` | Security, BCrypt bean, seeder, redirect handlers |
| `util` | Stateless helpers: `Timeslots`, `DateUtil` |

This separation ensures the matching algorithm in `MatchingService` can be tested without the HTTP stack, and security rules in `SecurityConfig` can change without touching business logic (Pivotal Software, 2024).

The three domain entities model the system's core objects. `User` stores identity, role (STUDENT or ADMIN), year group, exam board, and availability timeslots via `@ElementCollection`. `Request` records the offer or seek, its candidate timeslots, and — once matched — the chosen timeslot, week-start date, and matched partner. Relationships use `@ManyToMany` (User–Subject) and `@ManyToOne` (Request–User, Request–Subject), which Hibernate maps to normalised relational tables in PostgreSQL (production) or H2 (tests).

---

## Section 2: Key Techniques

### 2.1 Authentication and Authorisation (Spring Security + BCrypt)

Spring Security intercepts every HTTP request before any controller code executes and enforces URL-level and method-level access rules. Route protection is declared in `SecurityConfig.filterChain()`: `/admin/**` requires the `ADMIN` role; `/dashboard`, `/profile/**`, and `/requests/**` require `STUDENT`; public routes permit all. Individual controller methods are further annotated with `@PreAuthorize` as a second defence-in-depth layer.

Passwords are hashed with BCrypt via `BCryptPasswordEncoder` (configured in `PasswordConfig`). BCrypt incorporates a cost factor and a random salt, making brute-force and rainbow-table attacks computationally impractical even if the database is compromised (Williams and Balasundaram, 2020). Delegating these concerns to Spring Security means established, peer-reviewed implementations replace error-prone custom code.

> **Figure 1:** Screenshot of `SecurityConfig.filterChain()` method body.
> Source: `src/main/java/com/example/studentportal/config/SecurityConfig.java`, class `SecurityConfig`, method `filterChain`. Annotation: highlight the three `.requestMatchers(…).hasRole(…)` lines and the `.csrf(csrf -> csrf.disable())` line to illustrate role-based URL protection.

> **Figure 2:** Screenshot of the rendered login page in the browser.
> Source: `src/main/resources/templates/login.html`, rendered at `/login`. Annotation: draw an arrow to the form action pointing to `/login` and note that the password field is type="password" (never exposed in plaintext).

### 2.2 Transactional User Deletion with Cascading Cancellation

Deleting a user involves four ordered steps, all within a single `@Transactional` method (`UserService.deleteUser`): (1) cancel any `MATCHED` request belonging to a partner of the deleted user, setting status to `CANCELLED` and clearing the `matchedPartner` foreign key; (2) issue a JPQL bulk update to null remaining references on `DONE` requests; (3) delete all of the user's own requests; (4) delete the user record, which causes Hibernate to clean the `user_subjects` and `user_availability` join tables automatically. The `@Transactional` annotation ensures the sequence completes atomically or rolls back entirely, preserving database consistency (Johnson et al., 2022). The cascade order is deliberate: partner requests are cancelled before the user is deleted so that partners see a meaningful `CANCELLED` status rather than a missing record.

> **Figure 3:** Screenshot of `UserService.deleteUser()` method body.
> Source: `src/main/java/com/example/studentportal/service/UserService.java`, class `UserService`, method `deleteUser`. Annotation: number the four cascade steps with callouts (1) through (4) as described above.

---

## Section 3: Algorithmic Thinking

### 3.1 Maximum-Weight Bipartite Matching

**Input sets.** All `PENDING` requests are split into TUTOR offers (left partition) and TUTEE requests (right partition). Matching operates at the *(request, timeslot)* pair level: each such pair becomes a `RequestTimeslot` vertex. This models the reality that a TUTOR request with three available slots can be fulfilled at any one of them.

**Graph construction.** An edge is added between a TUTOR vertex and a TUTEE vertex only when four hard constraints hold: identical subject code, the same specific timeslot, different user accounts, and the tutor's year group ≥ the tutee's. The edge weight is computed in `calculateWeight()`: base 100 points, plus 50 for a matching exam board, plus a year-proximity bonus from 30 (gap = 1) down to 10 (gap = 4).

**Algorithm choice.** `MaximumWeightBipartiteMatching` from JGraphT (Michail et al., 2020) is applied to the graph. A greedy approach — always taking the highest-weight available edge — is suboptimal: an early greedy choice can block two lower-weight-but-feasible matches whose combined weight exceeds the greedy pair's. JGraphT's algorithm finds the globally optimal edge set in polynomial time via an augmenting-path strategy, acceptable because active requests are bounded by the school population.

**Post-processing pass.** A secondary greedy pass over edges sorted highest-weight first enforces per-request and per-user per-timeslot uniqueness, then persists the results.

> **Figure 4 (Diagram):** Flowchart of `MatchingService.runMatching()`.
> Caption: *Flowchart showing the five stages of the bipartite matching algorithm: (1) load PENDING requests, (2) build RequestTimeslot vertices, (3) add constrained weighted edges, (4) apply MaximumWeightBipartiteMatching, (5) greedy deduplication pass.*

> **Figure 5:** Screenshot of `MatchingService.runMatching()` and `calculateWeight()`.
> Source: `src/main/java/com/example/studentportal/service/MatchingService.java`, class `MatchingService`, methods `runMatching` and `calculateWeight`. Annotation: highlight the edge-weight formula lines and the `MaximumWeightBipartiteMatching` constructor call.

### 3.2 Automated MATCHED → DONE Transition

`RequestStatusScheduler.markCompletedRequestsDone()` fires via `@Scheduled(fixedDelay = 60_000)` every sixty seconds. For each `MATCHED` request, it calls `shouldBeMarkedDone(request, now)`.

The method reconstructs the session's end time from two stored fields: `weekStartDate` (Monday of the matched week) and `chosenTimeslot` (e.g., `TUE_P2`). `Timeslots.getTimeslotEndTime()` splits the code on `_P` to extract the day and period number, maps the day to an offset from Monday (0 for MON, 4 for FRI), retrieves the period end-clock from a static array, and calls `LocalDate.plusDays().atTime()` to produce a `LocalDateTime`. If `TimeService.now()` is strictly after this value the request is set to `DONE`; the strict `isAfter` ensures the session is not marked done while still running. Requests with `null` fields are skipped to prevent `NullPointerException`.

`TimeService` allows the clock to be frozen by setting `app.simulation.datetime` in the properties file, making scheduler behaviour deterministic in testing without altering production code (README_NEW.md, Geiger-J/studentportal).

> **Figure 6 (Diagram):** Step diagram of the MATCHED → DONE transition.
> Caption: *Step diagram showing inputs (weekStartDate, chosenTimeslot), the end-time calculation performed by `Timeslots.getTimeslotEndTime()`, the comparison with `TimeService.now()`, and the conditional write to the database.*

> **Figure 7:** Screenshot of `RequestStatusScheduler.markCompletedRequestsDone()` and `shouldBeMarkedDone()`.
> Source: `src/main/java/com/example/studentportal/service/RequestStatusScheduler.java`, class `RequestStatusScheduler`. Annotation: highlight the `@Scheduled` annotation and the `isAfter(endTime)` comparison.

> **Figure 8:** Screenshot of the student dashboard showing a request with status "Matched" and the chosen timeslot visible.
> Source: `src/main/resources/templates/dashboard.html`, rendered at `/dashboard`. Annotation: circle the matched partner name and chosen timeslot fields to show they are displayed to the student in real time.

---

## Section 4: Existing Tools, Libraries, and Infrastructure

| Tool / Library | Role | Justification |
|----------------|------|---------------|
| Spring Boot 3.5.4 (Pivotal Software, 2024) | Application framework | Auto-configuration and embedded Tomcat eliminate boilerplate server setup |
| Spring MVC (incl.) | HTTP layer | Annotation-driven controllers map routes to service calls with minimal configuration |
| Spring Security (incl.) | Authentication & authorisation | Peer-reviewed security framework managing sessions, BCrypt, and access rules |
| Spring Data JPA / Hibernate (incl.) | ORM & data access | Maps Java objects to relational tables; `@Transactional` enforces ACID properties |
| PostgreSQL (production) | Relational database | Production-grade RDBMS with foreign-key constraints; schema created automatically by Hibernate |
| H2 (test/dev) | In-memory relational database | Zero-configuration SQL database for CI tests and local development |
| Thymeleaf (incl.) | Server-side templating | Natural HTML templates that integrate with Spring Security for auth-aware rendering (Thymeleaf, 2024) |
| JGraphT 1.5.1 (Michail et al., 2020) | Graph algorithms | Provides `MaximumWeightBipartiteMatching`, avoiding manual implementation of a complex polynomial-time algorithm |
| thymeleaf-extras-springsecurity6 (incl.) | Template security | Enables `sec:authorize` directives for role-conditional UI rendering |

---

## Section 5: Testing Evidence

The test suite uses `@SpringBootTest` with the `test` Spring profile, substituting H2 for PostgreSQL and excluding `RequestStatusScheduler` via `@Profile("!test")`. All tests are `@Transactional`, rolling back after each case.

**`MatchingServiceTest`** verifies the core algorithm: a valid TUTOR/TUTEE pairing produces two `MATCHED` requests with correct `matchedPartner` values; non-overlapping timeslots and mismatched subjects each produce zero matches.

**`MatchingServiceTimeslotConflictTest`** checks the post-processing invariant: a user already matched in a timeslot must not receive a second match in that slot.

**`RequestStatusSchedulerTest`** tests `shouldBeMarkedDone()` as a unit test. Key boundary cases: past end time (mark done), exactly at end time (must not, `isAfter` is strict), null `weekStartDate` (skip), null `chosenTimeslot` (skip).

**`UserServiceTest`** includes `testDeleteUser_CancelsPartnerMatchedRequest` and `testDeleteUser_AlsoDeletesOwnRequests`, verifying cascading deletion: partner request `CANCELLED`, `matchedPartner` cleared, user's own requests removed.

**`SecurityConfigTest`** confirms unauthenticated access to protected routes redirects to `/login`.

---

## Bibliography

Geiger-J/studentportal (2025) *Student Portal — Technical Documentation* [Internal technical document]. `README_NEW.md`, repository: Geiger-J/studentportal. Available at: https://github.com/Geiger-J/studentportal (Accessed: 22 February 2026).

H2 Database Engine (2024) *H2 Database Engine Documentation*. Available at: https://www.h2database.com/html/main.html (Accessed: 22 February 2026).

Johnson, R., Hoeller, J., Donald, K., Sampaleanu, C., Harrop, R. and Beams, C. (2022) *Spring Framework Reference Documentation*, version 6. Pivotal Software. Available at: https://docs.spring.io/spring-framework/docs/current/reference/html/ (Accessed: 22 February 2026).

Michail, D., Kinable, J., Naveh, B. and Sichi, J. V. (2020) 'JGraphT — A Java library for graph data structures and algorithms', *ACM Transactions on Mathematical Software*, 46(2), pp. 1–26. doi: 10.1145/3381449.

Pivotal Software (2024) *Spring Boot Reference Documentation*, version 3.x. Available at: https://docs.spring.io/spring-boot/docs/current/reference/html/ (Accessed: 22 February 2026).

PostgreSQL Global Development Group (2024) *PostgreSQL 16 Documentation*. Available at: https://www.postgresql.org/docs/16/ (Accessed: 22 February 2026).

Thymeleaf (2024) *Thymeleaf Documentation*. Available at: https://www.thymeleaf.org/documentation.html (Accessed: 22 February 2026).

Williams, J. and Balasundaram, R. (2020) *Spring Security in Action*. Shelter Island, NY: Manning Publications.
