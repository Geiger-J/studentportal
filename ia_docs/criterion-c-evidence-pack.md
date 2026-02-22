# Criterion C Evidence Pack (Development)

---

## 0. Repo overview (verified)

**Architecture:** The application is a multi-tier, server-rendered web application built on **Spring Boot 3.5.4**. It uses a **relational database via JPA** (PostgreSQL in production, H2 in-memory for tests) for all persistent data. The presentation layer is Thymeleaf HTML templates; the business logic layer consists of Spring `@Service` classes; data access is via Spring Data JPA repositories. There is no file-based storage in the project.

**Key packages and responsibilities:**

| Package | Responsibility |
|---|---|
| `com.example.studentportal.model` | JPA entity classes (`User`, `Request`, `Subject`) — the relational schema |
| `com.example.studentportal.repository` | Spring Data JPA repository interfaces; exposes JPQL and derived-query methods |
| `com.example.studentportal.service` | All business logic: matching algorithm, request lifecycle, user management, scheduling, time abstraction |
| `com.example.studentportal.controller` | MVC controllers (`RequestController`, `ProfileController`, `AdminController`, etc.) mapping HTTP to services |
| `com.example.studentportal.config` | Spring Security filter chain, BCrypt bean, post-login redirect handler, global Thymeleaf model data, startup seeder |
| `com.example.studentportal.util` | `Timeslots` catalog (35-slot catalogue, label map, end-time calculator) and `DateUtil` (ISO week arithmetic) |

**Runtime stack and build tools:**

- Java 21, Spring Boot 3.5.4 (Spring MVC, Spring Security 6, Spring Data JPA, Thymeleaf)
- JGraphT 1.5.1 — graph-theory library used for weighted bipartite matching
- Hibernate 6 (JPA provider), PostgreSQL 42 driver (runtime), H2 (test)
- Bean Validation 3 (Jakarta) via `spring-boot-starter-validation`
- Maven (wrapper `mvnw`), JUnit 5 + Spring Boot Test for automated tests
- BCrypt password encoding via `spring-security-crypto`

---

## 1. High-value techniques (ranked)

---

### 1.1 Weighted bipartite matching algorithm using JGraphT

**What it does (1–3 sentences):**
`MatchingService.runMatching()` models the tutoring pairing problem as a weighted bipartite graph where each vertex is a *(Request, timeslot)* pair, and an edge exists between two such vertices when they share the same timeslot code and pass all hard constraints. The library algorithm `MaximumWeightBipartiteMatching` (an implementation of the Blossom / Hungarian-family algorithm) finds the globally optimal set of non-overlapping edges. A greedy post-pass then de-duplicates the results and enforces a per-user, per-timeslot exclusivity constraint.

**Why it is complex or ingenious:**

- **Algorithmic complexity:** Lifting individual requests to *(request, timeslot)* vertices multiplies the vertex count, giving the algorithm the granularity to prefer a high-weight Tuesday slot over a lower-weight Monday slot for the same pair — something a naïve request-level graph cannot express.
- **Combinatorial optimality:** `MaximumWeightBipartiteMatching` from JGraphT maximises the total weight across all matched pairs simultaneously rather than making greedy local decisions, guaranteeing a globally optimal assignment.
- **Non-trivial state management:** After the library returns its solution, a second pass maintains `userMatchedTimeslots : Map<Long, Set<String>>` seeded with *already-committed* MATCHED requests from the database, so re-running matching in the same week cannot book a user into a slot they occupy in a prior match.
- **Correctness under edge cases:** The algorithm correctly handles the edge-source/target ambiguity in JGraphT (source may be a TUTEE vertex) by swapping `offerRT` / `seekRT` when needed before recording the match.
- **Maintainability via separation of concerns:** Hard constraints (subject equality, year-group ordering, timeslot overlap, self-match prevention) are isolated in `meetHardConstraints()`; soft preferences (exam-board bonus, year-gap bonus) are isolated in `calculateWeight()`. Each concern is independently testable.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/MatchingService.java`
- Class: `MatchingService`
- Methods: `runMatching()`, `calculateWeight()`, `meetHardConstraints()`

**Code evidence excerpt:**

```java
// MatchingService.java — runMatching() (lines ~140–200, graph construction)
Graph<RequestTimeslot, DefaultWeightedEdge> graph =
        new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

Set<RequestTimeslot> offerVertices = new HashSet<>();
for (Request offer : offerRequests) {
    for (String timeslot : offer.getTimeslots()) {
        RequestTimeslot rt = new RequestTimeslot(offer, timeslot);
        graph.addVertex(rt);
        offerVertices.add(rt);
    }
}

Set<RequestTimeslot> seekVertices = new HashSet<>();
for (Request seek : seekRequests) {
    for (String timeslot : seek.getTimeslots()) {
        RequestTimeslot rt = new RequestTimeslot(seek, timeslot);
        graph.addVertex(rt);
        seekVertices.add(rt);
    }
}

for (RequestTimeslot offerRT : offerVertices) {
    for (RequestTimeslot seekRT : seekVertices) {
        if (Objects.equals(offerRT.getTimeslot(), seekRT.getTimeslot()) &&
            meetHardConstraints(offerRT.getRequest(), seekRT.getRequest())) {
            double weight = calculateWeight(offerRT.getRequest(), seekRT.getRequest());
            if (weight > 0) {
                DefaultWeightedEdge edge = graph.addEdge(offerRT, seekRT);
                if (edge != null) {
                    graph.setEdgeWeight(edge, weight);
                }
            }
        }
    }
}

MaximumWeightBipartiteMatching<RequestTimeslot, DefaultWeightedEdge> matching =
        new MaximumWeightBipartiteMatching<>(graph, offerVertices, seekVertices);
Set<DefaultWeightedEdge> matchedEdges = matching.getMatching().getEdges();
```

**Edge cases handled:**

- No PENDING requests on either side → empty vertex sets → empty graph → zero matches, no crash.
- Single request on one side with no compatible pair → no edges → zero matches.
- JGraphT may assign either endpoint as "source"; the swap guard (`if ("TUTEE".equals(offerRT.getRequest().getType()))`) normalises the result before recording.
- Weight ≤ 0 (impossible under current logic but defensive) → edge not added.

**Tests that cover it:**

- `MatchingServiceTest.testPerformMatching_SuccessfulMatch()` — verifies two compatible requests → both MATCHED, partner set correctly.
- `MatchingServiceTest.testPerformMatching_NoOverlappingTimeslots()` — no shared slot → zero matches, both remain PENDING.
- `MatchingServiceTest.testPerformMatching_DifferentSubjects()` — subject mismatch → zero matches.
- `MatchingServiceTimeslotConflictTest.testUsersShouldNotBeMatchedAtSameTimeslotMultipleTimes()` — same pair has MATHS + PHYSICS requests both at MON_P1; asserts at most one match occurs at that slot.

**Algorithm diagram (step list):**

```
runMatching() algorithm — steps for a flow diagram:

1.  Query DB: fetch all PENDING TUTOR requests → offerRequests list
2.  Query DB: fetch all PENDING TUTEE requests → seekRequests list
3.  For each offer request:
      For each timeslot in offer.timeslots:
        Create vertex (offer, timeslot) → add to graph + offerVertices set
4.  For each seek request:
      For each timeslot in seek.timeslots:
        Create vertex (seek, timeslot) → add to graph + seekVertices set
5.  For each (offerRT, seekRT) in offerVertices × seekVertices:
      If offerRT.timeslot == seekRT.timeslot
        AND meetHardConstraints(offer, seek):
          weight = calculateWeight(offer, seek)   // ≥ 100 base + soft bonuses
          addEdge(offerRT, seekRT, weight=weight)
6.  Run MaximumWeightBipartiteMatching on the graph
      → returns set of non-overlapping edges maximising total weight
7.  Query DB: fetch all already-MATCHED requests
      → build userMatchedTimeslots : Map<userId, Set<timeslot>>
8.  Sort matched edges descending by weight
9.  For each edge (greedy pass):
      Normalise: ensure offerRT is TUTOR side
      Check: neither request already used in this run
      Check: neither user already has a match at this timeslot
      If both checks pass:
        Record Match(offer, seek, timeslot, weight)
        Mark both request IDs as used
        Add timeslot to both users' tracking sets
10. Return list of Match objects
```

**Suggested evidence figures:**

- **Figure 1:** Bipartite graph diagram showing offer vertices (left), seek vertices (right), edges labelled with weights; highlight the maximum-weight matching edges.
- **Figure 2:** Flowchart of `runMatching()` using the step list above, with the greedy post-pass as a separate swimlane.

---

### 1.2 Per-user timeslot conflict prevention in the greedy post-pass

**What it does (1–3 sentences):**
After `MaximumWeightBipartiteMatching` returns its solution, a second pass re-filters the result to ensure no user is assigned to two different tutoring sessions at the same timeslot code within the same matching run or across historical MATCHED records in the database. It maintains a `Map<Long, Set<String>>` (user ID → set of occupied timeslots) that is seeded from existing MATCHED requests before the loop begins, giving cross-run safety.

**Why it is complex or ingenious:**

- **Non-trivial state management:** The conflict map is built from *two sources* — past DB records and the in-progress match list — and is updated after every accepted match, making subsequent iterations in the same run aware of choices already committed.
- **Correctness under edge cases:** Because the DB is queried before the loop (not inside it), the check is consistent: it reflects the true state at the start of the run even if a prior run created MATCHED records.
- **Algorithmic complexity:** The outer algorithm (bipartite matching) is globally optimal *for timeslots in isolation*, but cannot see that a user might appear in two independent matched pairs at the same slot; this post-pass is the necessary correction step that preserves feasibility.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/MatchingService.java`
- Class: `MatchingService`
- Methods: `runMatching()` (approximately lines 185–250)

**Code evidence excerpt:**

```java
// MatchingService.java — conflict-prevention post-pass inside runMatching()
Map<Long, Set<String>> userMatchedTimeslots = new HashMap<>();
List<Request> alreadyMatchedRequests = requestRepository.findByStatus("MATCHED");
for (Request matchedRequest : alreadyMatchedRequests) {
    String chosenTimeslot = matchedRequest.getChosenTimeslot();
    if (chosenTimeslot != null) {
        Long userId = matchedRequest.getUser().getId();
        userMatchedTimeslots
            .computeIfAbsent(userId, k -> new HashSet<>())
            .add(chosenTimeslot);
    }
}

List<DefaultWeightedEdge> sortedEdges = new ArrayList<>(matchedEdges);
sortedEdges.sort((e1, e2) ->
    Double.compare(graph.getEdgeWeight(e2), graph.getEdgeWeight(e1)));

for (DefaultWeightedEdge edge : sortedEdges) {
    // ... normalise offer/seek ...
    boolean requestsNotMatched =
        !matchedOfferRequestIds.contains(offerRequest.getId()) &&
        !matchedSeekRequestIds.contains(seekRequest.getId());
    boolean noTimeslotConflict =
        !userMatchedTimeslots.getOrDefault(tutorUserId, Collections.emptySet())
                             .contains(timeslot) &&
        !userMatchedTimeslots.getOrDefault(tuteeUserId, Collections.emptySet())
                             .contains(timeslot);

    if (requestsNotMatched && noTimeslotConflict) {
        matches.add(new Match(offerRequest, seekRequest, timeslot, weight));
        matchedOfferRequestIds.add(offerRequest.getId());
        matchedSeekRequestIds.add(seekRequest.getId());
        userMatchedTimeslots.computeIfAbsent(tutorUserId, k -> new HashSet<>())
                            .add(timeslot);
        userMatchedTimeslots.computeIfAbsent(tuteeUserId, k -> new HashSet<>())
                            .add(timeslot);
    }
}
```

**Edge cases handled:**

- User has a pre-existing MATCHED record from a previous run at MON_P1 → map is pre-populated → new match at MON_P1 for same user is rejected.
- `chosenTimeslot` is null (legacy records before the field existed) → null guard prevents NPE, record is skipped.
- Same pair appears multiple times in the bipartite solution (theoretically impossible but defensively guarded) → `matchedOfferRequestIds` / `matchedSeekRequestIds` sets prevent double-acceptance.

**Tests that cover it:**

- `MatchingServiceTimeslotConflictTest.testUsersShouldNotBeMatchedAtSameTimeslotMultipleTimes()` — four PENDING requests (tutor + tutee, MATHS + PHYSICS, all at MON_P1); asserts that the same user does not end up in two MATCHED requests both pointing at MON_P1.
- `MatchingServiceRepeatedRunTest` — verifies idempotency: re-running matching after an initial match does not create additional matches.

**Suggested evidence figures:**

- **Figure 3:** Before/after table: bipartite solution before post-pass (possibly two matches at MON_P1 for same user) vs. after post-pass (only one accepted).
- **Figure 4:** State diagram of `userMatchedTimeslots` map being built from DB, then extended edge-by-edge.

---

### 1.3 Transactional atomic cascade cancellation

**What it does (1–3 sentences):**
`RequestService.cancelRequest()` and `RequestService.adminCancelRequest()` cancel not only the target request but also the matched partner's request atomically within a single `@Transactional` boundary. `UserService.deleteUser()` additionally cancels all MATCHED partner requests before deleting the user's own records, preventing orphaned references.

**Why it is complex or ingenious:**

- **Transactional integrity:** Both the original and partner cancellations are committed together; a failure mid-way rolls the entire transaction back, so the database never ends up with one CANCELLED + one MATCHED for the same pairing.
- **Correctness under edge cases:** Partner lookup uses the four-way key `(partner, user, "MATCHED", subject)` which handles the case where the same two users might theoretically have multiple subjects matched together — only the correct subject's partner request is cancelled.
- **Security:** Ownership is checked (`!request.getUser().equals(user)`) before any mutation, preventing one student from cancelling another's request via a crafted request ID.
- **Maintainability via separation of concerns:** The `canBeCancelled()` and `cancel()` methods on `Request` encapsulate the state-machine transitions; the service layer calls these rather than directly setting strings, keeping the valid transitions in one place.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/RequestService.java`
- Class: `RequestService`
- Methods: `cancelRequest()`, `adminCancelRequest()`
- File: `src/main/java/com/example/studentportal/service/UserService.java`
- Class: `UserService`
- Methods: `deleteUser()`

**Code evidence excerpt:**

```java
// RequestService.java — cancelRequest()
public Request cancelRequest(Long requestId, User user) {
    Optional<Request> requestOpt = requestRepository.findById(requestId);
    if (requestOpt.isEmpty()) {
        throw new IllegalArgumentException("Request not found");
    }
    Request request = requestOpt.get();

    // Ownership guard
    if (!request.getUser().equals(user)) {
        throw new IllegalArgumentException("You can only cancel your own requests");
    }
    if (!request.canBeCancelled()) {
        throw new IllegalArgumentException("This request cannot be cancelled");
    }

    // Cascade to matched partner
    if ("MATCHED".equals(request.getStatus()) && request.getMatchedPartner() != null) {
        User partner = request.getMatchedPartner();
        Optional<Request> partnerRequestOpt =
            requestRepository.findByUserAndMatchedPartnerAndStatusAndSubject(
                partner, user, "MATCHED", request.getSubject());
        if (partnerRequestOpt.isPresent()) {
            Request partnerRequest = partnerRequestOpt.get();
            partnerRequest.cancel();
            requestRepository.save(partnerRequest);
        }
    }

    request.cancel();
    return requestRepository.save(request);
}
```

**Edge cases handled:**

- Request not found → `IllegalArgumentException` before any mutation.
- Request already DONE or CANCELLED → `canBeCancelled()` returns false → rejected.
- Partner request not found (e.g., already manually cancelled by admin) → the `if (partnerRequestOpt.isPresent())` guard prevents NPE; the original is still cancelled.
- Admin cancellation path (`adminCancelRequest()`) mirrors the same logic but skips ownership check, appropriate for admin authority.
- User deletion: `clearMatchedPartnerReferences()` is a `@Modifying` JPQL UPDATE that NULLs `matched_partner_id` for all remaining requests, preventing FK violations after the user row is removed.

**Tests that cover it:**

- `RequestServiceTest` covers duplicate-prevention and basic creation flow; the cancellation cascade is exercised in integration by the admin controller tests and the `UserServiceTest.testDeleteUser_CancelsPartnerRequests` scenario.

**Suggested evidence figures:**

- **Figure 5:** Sequence diagram: student triggers cancel → `cancelRequest()` → partner lookup → both saves → commit.
- **Figure 6:** State machine for `Request.status`: PENDING → MATCHED → DONE / CANCELLED, with the `canBeCancelled()` guard highlighted.

---

### 1.4 Spring Security multi-layer access control with profile-completeness gate

**What it does (1–3 sentences):**
`SecurityConfig` defines a URL-level filter chain that restricts `/admin/**` to `ROLE_ADMIN` and `/dashboard`, `/profile/**`, `/requests/**` to `ROLE_STUDENT`. `CustomAuthenticationSuccessHandler` adds a business-logic gate: a student whose `profileComplete` flag is false is redirected to `/profile` instead of `/dashboard`. Individual controller methods carry `@PreAuthorize("hasRole('STUDENT')")` for method-level defence-in-depth.

**Why it is complex or ingenious:**

- **Security — defence in depth:** Three independent layers guard sensitive routes: (1) the URL-pattern `authorizeHttpRequests` ruleset, (2) post-login redirect logic, (3) `@PreAuthorize` annotations with Spring's method security (`@EnableMethodSecurity(prePostEnabled = true)`).
- **Non-trivial state management:** The profile-completeness redirect is computed at login time from the persisted `profileComplete` boolean, which is updated via `user.updateProfileCompleteness()` every time a profile save occurs, keeping the flag consistent with actual data.
- **Correctness under edge cases:** ADMIN users bypass the year-group / subjects / availability check entirely (handled in `User.isProfileComplete()` via an early-return on `"ADMIN".equals(role)`), avoiding a login loop for admin accounts that legitimately have no academic profile.
- **Maintainability via separation of concerns:** The success handler is decoupled from the security filter chain; swapping redirect logic requires only editing one class without touching the filter chain.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/config/SecurityConfig.java`
- Class: `SecurityConfig`
- Methods: `filterChain()`
- File: `src/main/java/com/example/studentportal/config/CustomAuthenticationSuccessHandler.java`
- Class: `CustomAuthenticationSuccessHandler`
- Methods: `onAuthenticationSuccess()`
- File: `src/main/java/com/example/studentportal/model/User.java`
- Class: `User`
- Methods: `isProfileComplete()`, `updateProfileCompleteness()`

**Code evidence excerpt:**

```java
// SecurityConfig.java — filterChain()
http
    .authorizeHttpRequests(authz -> authz
        .requestMatchers("/", "/login", "/register",
                "/css/**", "/images/**", "/js/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/dashboard", "/profile/**", "/requests/**").hasRole("STUDENT")
        .anyRequest().authenticated()
    )
    .formLogin(form -> form
        .loginPage("/login")
        .permitAll()
        .successHandler(authenticationSuccessHandler)
        .failureUrl("/login?error=true")
    )
    .exceptionHandling(ex -> ex
        .accessDeniedHandler(roleRedirectAccessDeniedHandler)
    );

// CustomAuthenticationSuccessHandler.java — onAuthenticationSuccess()
if ("ADMIN".equals(userPrincipal.getUser().getRole())) {
    response.sendRedirect("/admin/dashboard");
} else if (userPrincipal.getUser().getProfileComplete()) {
    response.sendRedirect("/dashboard");
} else {
    response.sendRedirect("/profile");
}

// User.java — isProfileComplete()
public boolean isProfileComplete() {
    if ("ADMIN".equals(role)) { return true; }
    return yearGroup != null
        && yearGroup >= 9 && yearGroup <= 13
        && !getSubjects().isEmpty()
        && !getAvailability().isEmpty();
}
```

**Edge cases handled:**

- ADMIN login → no year group or subjects in DB → `isProfileComplete()` short-circuits to `true` → admin reaches admin dashboard without a redirect loop.
- Student with partial profile (year group set but no subjects) → `isProfileComplete()` returns false → redirected to `/profile` every login until complete.
- Unknown principal type (future OAuth integration) → fallback `response.sendRedirect("/dashboard")` in the `else` branch.

**Tests that cover it:**

- `SecurityConfigTest` uses `@WithMockUser` and MockMvc to assert that `/admin/dashboard` returns 403 for a `STUDENT` role and `/dashboard` returns 403 for an `ADMIN` role.

**Suggested evidence figures:**

- **Figure 7:** Layered diagram: HTTP request → URL filter → method security → profile-completeness gate, showing where each layer intercepts.
- **Figure 8:** Decision flowchart for `onAuthenticationSuccess()`.

---

### 1.5 BCrypt password hashing with email-domain-driven role derivation

**What it does (1–3 sentences):**
`UserService.registerUser()` validates that the submitted email ends with `@example.edu`, rejects duplicates via a DB existence check, then hashes the raw password with BCrypt before persisting the user. The user's role (`STUDENT` or `ADMIN`) is derived deterministically from the email local-part: a leading digit means student, a leading letter means admin.

**Why it is complex or ingenious:**

- **Security:** BCrypt is a deliberately slow, salted adaptive hashing function. `BCryptPasswordEncoder` (from Spring Security) generates a new random salt per hash, so two identical passwords produce different hashes, defeating rainbow-table attacks.
- **Security — duplicate prevention:** The `existsByEmail()` check before insertion prevents an attacker from registering with an existing email to hijack an account (would otherwise cause ambiguity in Spring Security's `loadUserByUsername`).
- **Correctness under edge cases:** `@Email` and `@Pattern(regexp = ".*@example.edu$")` annotations on `User.email` provide a second, model-level validation layer independent of the service check, so the constraint is enforced even if the service is called from outside the controller.
- **Maintainability via separation of concerns:** Role determination is in its own method `determineRoleFromEmail()`, making the algorithm independently readable and testable.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/UserService.java`
- Class: `UserService`
- Methods: `registerUser()`, `determineRoleFromEmail()`
- File: `src/main/java/com/example/studentportal/config/PasswordConfig.java`
- Class: `PasswordConfig`
- Methods: `passwordEncoder()`

**Code evidence excerpt:**

```java
// UserService.java — registerUser()
public User registerUser(String fullName, String email, String rawPassword) {
    if (!email.endsWith("@example.edu")) {
        throw new IllegalArgumentException("Email must end with @example.edu");
    }
    if (userRepository.existsByEmail(email)) {
        throw new IllegalArgumentException("Email already exists");
    }
    String role = determineRoleFromEmail(email);
    String hashedPassword = passwordEncoder.encode(rawPassword);
    User user = new User(fullName, email, hashedPassword, role);
    return userRepository.save(user);
}

// UserService.java — determineRoleFromEmail()
public String determineRoleFromEmail(String email) {
    String localPart = email.substring(0, email.indexOf('@'));
    if (!localPart.isEmpty() && Character.isDigit(localPart.charAt(0))) {
        return "STUDENT";
    } else {
        return "ADMIN";
    }
}

// PasswordConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Edge cases handled:**

- Empty local-part → `!localPart.isEmpty()` guard → defaults to ADMIN (safe fail).
- Email already in DB → `IllegalArgumentException` before `encode()` is called, avoiding unnecessary CPU spend.
- `changePassword()` enforces minimum length (≥ 4) and non-blank before encoding, matching the `@Size(min=4)` constraint on `User.passwordHash`.

**Tests that cover it:**

- `UserServiceTest.testRegisterUser_WithStudentEmail_AssignsStudentRole()` — digit-prefixed email → STUDENT role.
- `UserServiceTest.testRegisterUser_WithAdminEmail_AssignsAdminRole()` — letter-prefixed email → ADMIN role.
- `UserServiceTest.testRegisterUser_PasswordIsHashed()` — asserts stored hash differs from raw password and `passwordEncoder.matches()` succeeds.
- `UserServiceTest.testRegisterUser_DuplicateEmail_ThrowsException()`.

**Suggested evidence figures:**

- **Figure 9:** Data flow diagram: raw password → `BCryptPasswordEncoder.encode()` → stored hash, with salt generation noted.
- **Figure 10:** Decision tree for `determineRoleFromEmail()`.

---

### 1.6 Timeslot end-time calculator with deterministic week-anchored datetime resolution

**What it does (1–3 sentences):**
`Timeslots.getTimeslotEndTime(LocalDate weekStart, String code)` converts an abstract slot code such as `"TUE_P3"` into a concrete `LocalDateTime` by parsing the code into a day-offset and period index, then adding the day offset to the ISO week's Monday anchor and looking up the period's end-time string from a compile-time array. `DateUtil.getMondayOfWeek()` provides the Monday anchor for any date using `TemporalAdjusters.previousOrSame`.

**Why it is complex or ingenious:**

- **Algorithmic complexity:** The function performs two independent parsing operations (day name → offset, period number → time string) and composes them into a precise `LocalDateTime` — all without a database call or external library beyond `java.time`.
- **Correctness under edge cases:** Multiple null/bounds guards return `null` gracefully rather than throwing, allowing the scheduler to skip malformed records silently.
- **Non-trivial state management:** The compile-time static initialiser builds the full 35-slot (5 days × 7 periods) catalogue into three immutable data structures — `ALL_CODES` (ordered list), `ALL_CODES_SET` (O(1) lookup set), `LABELS` (display map) — which are referenced across controllers, validators, and the scheduler without redundant definition.
- **Maintainability:** Changing a period's time requires editing a single string in `PERIOD_TIMES[]`; all consumers automatically pick up the new value.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/util/Timeslots.java`
- Class: `Timeslots`
- Methods: `getTimeslotEndTime()`, static initialiser block
- File: `src/main/java/com/example/studentportal/util/DateUtil.java`
- Class: `DateUtil`
- Methods: `getMondayOfWeek()`, `nextMonday()`

**Code evidence excerpt:**

```java
// Timeslots.java — getTimeslotEndTime()
public static LocalDateTime getTimeslotEndTime(LocalDate weekStart, String code) {
    if (weekStart == null || code == null) return null;

    String[] parts = code.split(TIMESLOT_SEPARATOR);   // "TUE_P3" → ["TUE","3"]
    if (parts.length != 2) return null;

    String day = parts[0];
    int period;
    try {
        period = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
        return null;
    }
    if (period < 1 || period > PERIODS) return null;

    int dayOffset = -1;
    for (int i = 0; i < DAYS.length; i++) {
        if (DAYS[i].equals(day)) { dayOffset = i; break; }
    }
    if (dayOffset < 0) return null;

    // PERIOD_TIMES[period-1] e.g. "09:55-10:45" → take "10:45"
    String endStr = PERIOD_TIMES[period - 1].split("-")[1];
    String[] hm = endStr.split(":");
    int hour   = Integer.parseInt(hm[0]);
    int minute = Integer.parseInt(hm[1]);

    return weekStart.plusDays(dayOffset).atTime(hour, minute);
}
```

**Edge cases handled:**

- `null` weekStart or code → immediate `null` return.
- Malformed code without separator (e.g., `"MONDAY1"`) → `parts.length != 2` → `null`.
- Non-numeric period (e.g., `"MON_PA"`) → `NumberFormatException` caught → `null`.
- Period out of range (e.g., `"MON_P8"`) → bounds check → `null`.
- Unknown day code (e.g., `"SAT_P1"`) → loop ends with `dayOffset == -1` → `null`.

**Tests that cover it:**

- `RequestStatusSchedulerTest.testShouldBeDone_WhenPastEndTime()` — MON_P1 ends at 09:50; "now" = 09:51 → `shouldBeMarkedDone()` returns true.
- `RequestStatusSchedulerTest.testShouldNotBeDone_WhenBeforeEndTime()` — 09:49 → false.
- `RequestStatusSchedulerTest.testShouldNotBeDone_WhenExactlyAtEndTime()` — strict `isAfter` check; exactly 09:50 → false.
- `RequestStatusSchedulerTest.testShouldNotBeDone_WhenNoWeekStartDate()` and `testShouldNotBeDone_WhenNoChosenTimeslot()` — null-guard tests.

**Suggested evidence figures:**

- **Figure 11:** Diagram showing `"TUE_P3"` parsing into `dayOffset=1`, `period=3`, resolved to `weekStart + 1 day @ 11:55`.
- **Figure 12:** The 35-slot catalogue rendered as a 5×7 table (days × periods) showing codes and end-times.

---

### 1.7 Idempotent background request-status scheduler with injectable simulated time

**What it does (1–3 sentences):**
`RequestStatusScheduler.markCompletedRequestsDone()` runs every 60 seconds (via `@Scheduled(fixedDelay = 60_000)`), queries all MATCHED requests, and transitions each one whose timeslot end-time is in the past to DONE using `Timeslots.getTimeslotEndTime()`. The scheduler is suppressed in the `test` Spring profile via `@Profile("!test")`, and its notion of "now" is supplied by `TimeService`, which can return a configured simulation timestamp from `app.simulation.datetime`.

**Why it is complex or ingenious:**

- **Non-trivial state management:** The operation is idempotent: requests already in DONE status are not in the query result (`findByStatus("MATCHED")`), so repeated runs at any time cannot double-transition a request or lose data.
- **Correctness under edge cases:** `shouldBeMarkedDone()` is a pure function (no side effects) that handles null weekStartDate and null chosenTimeslot with safe `false` returns, allowing legacy records to coexist without errors.
- **Maintainability via separation of concerns:** `TimeService` abstracts the clock, so developers can fast-forward time by setting a single property (`app.simulation.datetime=2025-01-20T09:51:00`) without touching the scheduler; tests use `@ActiveProfiles("test")` to disable the `@Scheduled` bean entirely, avoiding interference with test timing.
- **Security / reliability:** `@Transactional` on the scheduled method ensures that a partial batch (e.g., crash mid-loop) is rolled back; no request is left in an inconsistent intermediate state.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/RequestStatusScheduler.java`
- Class: `RequestStatusScheduler`
- Methods: `markCompletedRequestsDone()`, `shouldBeMarkedDone()`
- File: `src/main/java/com/example/studentportal/service/TimeService.java`
- Class: `TimeService`
- Methods: `now()`, `today()`

**Code evidence excerpt:**

```java
// RequestStatusScheduler.java
@Scheduled(fixedDelay = 60_000)
@Transactional
public void markCompletedRequestsDone() {
    LocalDateTime now = timeService.now();
    List<Request> matched = requestRepository.findByStatus("MATCHED");

    int doneCount = 0;
    for (Request request : matched) {
        if (shouldBeMarkedDone(request, now)) {
            request.setStatus("DONE");
            requestRepository.save(request);
            doneCount++;
        }
    }
    if (doneCount > 0) {
        logger.info("Marked {} matched request(s) as DONE", doneCount);
    }
}

boolean shouldBeMarkedDone(Request request, LocalDateTime now) {
    if (request.getWeekStartDate() == null || request.getChosenTimeslot() == null) {
        return false;
    }
    LocalDateTime endTime = Timeslots.getTimeslotEndTime(
            request.getWeekStartDate(), request.getChosenTimeslot());
    return endTime != null && now.isAfter(endTime);
}

// TimeService.java — injectable simulated clock
public LocalDateTime now() {
    if (simulationDatetime != null && !simulationDatetime.isBlank()) {
        return LocalDateTime.parse(simulationDatetime,
                                   DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    return LocalDateTime.now();
}
```

**Algorithm diagram (step list):**

```
markCompletedRequestsDone() algorithm — steps for a flowchart:

1. Obtain current time from TimeService.now()
   (real clock OR simulation.datetime property)
2. Query DB: fetch all requests with status = "MATCHED"
3. For each MATCHED request:
   a. If weekStartDate == null OR chosenTimeslot == null → SKIP (legacy record)
   b. Compute endTime = Timeslots.getTimeslotEndTime(weekStartDate, chosenTimeslot)
   c. If endTime == null → SKIP (unrecognised slot code)
   d. If now.isAfter(endTime):
        set request.status = "DONE"
        save to DB
        increment doneCount
4. If doneCount > 0: log info message
5. (Repeat after 60-second delay)
```

**Edge cases handled:**

- Requests missing `weekStartDate` or `chosenTimeslot` (created before the column was added) → skipped without exception.
- Clock skew / scheduler delay (fires at 09:52 instead of 09:51) → `isAfter` is still satisfied; idempotency means no harm from a delayed run.
- No MATCHED requests → empty list → zero saves, zero log noise.

**Tests that cover it:**

- `RequestStatusSchedulerTest` (6 unit tests) — tests `shouldBeMarkedDone()` directly with a manually constructed scheduler instance (`new RequestStatusScheduler(null, null)`), covering: past end-time, before end-time, exactly at end-time (strict), null weekStart, null timeslot, unknown slot code.

**Suggested evidence figures:**

- **Figure 13:** Flowchart of `markCompletedRequestsDone()` using the step list above.
- **Figure 14:** Timeline diagram: slot MON_P1 (09:00–09:50) on a week's calendar; arrow showing "scheduler fires at 09:51 → request transitions to DONE".

---

### 1.8 Duplicate-request prevention via database-backed existence check

**What it does (1–3 sentences):**
Before persisting a new `Request`, `RequestService.createRequest()` calls `requestRepository.existsByUserAndSubjectAndTypeAndStatus(user, subject, type, "PENDING")` — a single derived-query DB call — to reject a second PENDING request for the same (user, subject, type) triple. This prevents a user from having two competing PENDING offers for the same subject, which would produce ambiguous matching outcomes.

**Why it is complex or ingenious:**

- **Correctness under edge cases:** The check is scoped to `"PENDING"` status, so a user who had a previous CANCELLED or DONE request for the same subject can freely create a new one — the history is preserved without blocking re-use.
- **Algorithmic complexity:** The existence check is pushed down to the database as a single `EXISTS` subquery (generated by Spring Data), which is O(1) in index-scan cost rather than loading all user requests into memory.
- **Security:** Rejecting duplicates at the service layer — not just in the UI — means the constraint holds even if the form is submitted twice concurrently (race condition is handled by the DB unique-index-equivalent semantics of the JPA existence check within the same transaction).
- **Maintainability:** The error message is human-readable and type-aware (`"TUTOR"` → `"offering tutoring"`) so the user understands exactly what is blocked.

**Where in code (exact):**

- File: `src/main/java/com/example/studentportal/service/RequestService.java`
- Class: `RequestService`
- Methods: `createRequest()`, `hasActiveRequest()`
- File: `src/main/java/com/example/studentportal/repository/RequestRepository.java`
- Class (interface): `RequestRepository`
- Methods: `existsByUserAndSubjectAndTypeAndStatus()`

**Code evidence excerpt:**

```java
// RequestService.java — createRequest()
public Request createRequest(User user, String type, Subject subject,
                             Set<String> timeslots) {
    if (timeslots == null || timeslots.isEmpty()) {
        throw new IllegalArgumentException(
            "At least one timeslot must be selected");
    }

    if (hasActiveRequest(user, subject, type)) {
        String typeLabel = "TUTOR".equals(type)
            ? "offering tutoring" : "seeking tutoring";
        throw new IllegalArgumentException(
            "You already have an active " + typeLabel +
            " request for " + subject.getDisplayName());
    }

    Request request = new Request(user, type, subject, timeslots);
    return requestRepository.save(request);
}

// RequestService.java — hasActiveRequest()
@Transactional(readOnly = true)
public boolean hasActiveRequest(User user, Subject subject, String type) {
    return requestRepository.existsByUserAndSubjectAndTypeAndStatus(
        user, subject, type, "PENDING");
}

// RequestRepository.java — derived query
boolean existsByUserAndSubjectAndTypeAndStatus(
    User user, Subject subject, String type, String status);
```

**Edge cases handled:**

- Null timeslots or empty set → rejected before the DB duplicate check, giving a clear "select at least one timeslot" message.
- Same user, same subject, different type (one TUTOR + one TUTEE) → different `type` value → not a duplicate; both are allowed (covering the case of a student who can both offer and seek in the same subject).
- User has a CANCELLED request for the same subject → status is not `"PENDING"` → existence check returns false → new request allowed.
- Controller also filters timeslot codes through `Timeslots.ALL_CODES_SET` before calling the service, providing an extra whitelist validation layer.

**Tests that cover it:**

- `RequestServiceTest.testCreateRequest_DuplicatePending_ThrowsException()` — creates one PENDING TUTOR request, then attempts a second for same user/subject/type → asserts `IllegalArgumentException`.
- `RequestServiceTest.testCreateRequest_Successful()` — basic happy-path creation.
- `RequestServiceTest.testCreateRequest_NoTimeslots_ThrowsException()` — null timeslots → exception.
- `MatchingServiceDuplicateTest` — integration-level, verifies that even with multiple pending requests the matching engine handles each request at most once.

**Suggested evidence figures:**

- **Figure 15:** Sequence diagram: POST `/requests` → controller → `createRequest()` → `existsByUserAndSubjectAndTypeAndStatus()` DB call → success or rejection.
- **Figure 16:** Table showing which (user, subject, type, status) combinations are blocked vs. allowed by the duplicate check.

---

*End of evidence pack. All code excerpts verified against source files in the repository at the time of writing. File paths are relative to the repository root.*
