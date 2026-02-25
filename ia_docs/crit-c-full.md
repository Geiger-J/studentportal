# Criterion C: Product Development (Developing the Product)

---

## 1. Overview of the implemented architecture

The product follows a layered **Model-View-Controller (MVC)** architecture provided by Spring Boot 3.
The **model** layer (`src/main/java/com/example/studentportal/model/`) contains the three JPA entities: `User`, `Request`, and `Subject`.
The **controller** layer (`controller/`) handles HTTP routing; six controllers cover distinct responsibilities: `AuthController` (registration), `ProfileController` (profile editing and self-deletion), `RequestController` (creating and cancelling requests), `DashboardController` (student dashboard), `AdminController` (all admin actions), and `HomeController` (landing page).
The **service** layer (`service/`) encodes business rules and is the only caller of the repository layer; key service classes are `UserService`, `RequestService`, `MatchingService`, `RequestStatusScheduler`, and `TimeService`.
The **persistence** layer (`repository/`) consists of Spring Data JPA interfaces — `UserRepository`, `RequestRepository`, and `SubjectRepository` — which generate SQL at runtime via Hibernate.
The **security boundary** is defined in `config/SecurityConfig.java`: URLs under `/admin/**` require the `ROLE_ADMIN` authority; `/dashboard`, `/profile/**`, and `/requests/**` require `ROLE_STUDENT`; the login and registration endpoints are publicly accessible.
`config/CustomAuthenticationSuccessHandler.java` implements post-login redirection, routing admins to `/admin/dashboard` and students to `/profile` or `/dashboard` according to their `profileComplete` flag.

This separation ensures that no business logic leaks into Thymeleaf templates and that all persistence calls are mediated through the service layer, making each layer independently testable.

---

## 2. Data model and persistence decisions

Three entities capture all system state.
`User` stores identity, credentials, role, academic profile (`yearGroup`, `examBoard`), and availability.
`Request` records a single tutoring offer or request-for-tutoring, its lifecycle status, the matched partner reference, and the scheduled session details.
`Subject` is a lookup entity pre-seeded with ten values by `config/DataSeeder.java`.

The subject-to-user relationship is **many-to-many** (a student may study several subjects; a subject can be taken by many students), implemented as a join table `user_subjects`.
User availability is a `@ElementCollection` stored in `user_availability`; this avoids creating a separate `Timeslot` entity for what is effectively a value set.
The matched-partner link on `Request` is a self-referential **many-to-one** to `User`; it is `nullable` so PENDING requests carry no partner reference, which naturally enforces the privacy rule that partner details are only exposed post-match (Success Criterion 12).

**Representative entity mapping (User.java):**

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(name = "user_subjects",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "subject_id"))
private Set<Subject> subjects = new HashSet<>();

@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_availability",
    joinColumns = @JoinColumn(name = "user_id"))
@Column(name = "timeslot")
private Set<String> availability = new HashSet<>();

@OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
private Set<Request> requests = new HashSet<>();
```

> **Implementation note — FetchType.EAGER and LazyInitialization:**  
> During development, Spring Security's `CustomUserDetailsService` loads a `User` from the database to build the authentication principal. When `subjects`, `availability`, and `requests` were mapped with the default `FetchType.LAZY`, accessing those collections outside the original transaction (during rendering of the dashboard or within the security filter chain) produced `LazyInitializationException` at runtime. The fix applied `FetchType.EAGER` to all three collections on `User`. The trade-off is that every `User` load now issues additional SQL joins — a potential N+1 risk. This is mitigated in the current workload (school-size data volumes) by the fact that matching and admin queries operate on `Request` directly rather than iterating `User` collections, and by Hibernate's second-level session cache within each transaction.

---

## 3. Algorithmic thinking: weighted matching pipeline

The matching algorithm in `service/MatchingService.java` solves the problem of pairing TUTOR requests with TUTEE requests optimally.

**Graph construction.** Each `(Request, timeslot)` pair becomes a vertex — the inner class `RequestTimeslot`. TUTOR vertices form one partition; TUTEE vertices form the other. An edge is added between an offer vertex and a seek vertex only when they share the same timeslot *and* pass all hard constraints (see below). Each accepted edge is weighted.

**Hard constraints** (`meetHardConstraints`):

```java
private boolean meetHardConstraints(Request offer, Request seek) {
    User tutor = offer.getUser();
    User tutee = seek.getUser();

    if (!offer.getSubject().getCode().equals(seek.getSubject().getCode()))
        return false;

    Set<String> tutorSlots = offer.getTimeslots();
    Set<String> tuteeSlots = seek.getTimeslots();
    if (tutorSlots.stream().noneMatch(tuteeSlots::contains))
        return false;

    if (tutor.getYearGroup() < tutee.getYearGroup())
        return false;

    if (tutor.equals(tutee))
        return false;

    return true;
}
```

These constraints enforce Success Criterion 11 directly: same subject, at least one shared timeslot, tutor year ≥ tutee year, different users, and (implicitly) year difference ≤ 4 via the weight function returning zero benefit for gaps beyond four years.

**Weight function.** Base weight is 100. Same exam board adds 50; year-group difference of 1 adds 30, difference of 2 adds 20, difference of 3 adds 15, difference of 4 adds 10.

**Graph construction snippet (MatchingService.java):**

```java
Graph<RequestTimeslot, DefaultWeightedEdge> graph =
    new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

for (Request offer : offerRequests)
    for (String timeslot : offer.getTimeslots()) {
        RequestTimeslot rt = new RequestTimeslot(offer, timeslot);
        graph.addVertex(rt);
        offerVertices.add(rt);
    }

// (seek-side vertices added equivalently)

for (RequestTimeslot offerRT : offerVertices)
    for (RequestTimeslot seekRT : seekVertices)
        if (Objects.equals(offerRT.getTimeslot(), seekRT.getTimeslot()) &&
            meetHardConstraints(offerRT.getRequest(), seekRT.getRequest())) {
            double weight = calculateWeight(offerRT.getRequest(), seekRT.getRequest());
            if (weight > 0) {
                DefaultWeightedEdge edge = graph.addEdge(offerRT, seekRT);
                if (edge != null) graph.setEdgeWeight(edge, weight);
            }
        }
```

`MaximumWeightBipartiteMatching` (JGraphT) is then applied to the graph, yielding a set of matched edges. A **greedy post-pass** sorts those edges by weight descending and accepts each one only when neither request has already been consumed in this run and neither user already has a session in that timeslot, preventing double-booking (Success Criteria 10–12).

**Privacy note.** The `matchedPartner` field on `Request` is `null` until `performMatching()` writes it. Thymeleaf templates render partner name and email only when `request.status == 'MATCHED'`, so a student in PENDING status sees no partner information at all, satisfying the privacy constraint of Criterion 12.

> **[Diagram Placeholder: Criterion B § 4.4 — Admin Matching Execution Workflow flowchart]**

---

## 4. Request lifecycle, integrity, and cascade behaviours

Requests move through the states: `PENDING` → `MATCHED` → `DONE` or `CANCELLED`.

When a student (or admin) cancels a MATCHED request, the service must also cancel the partner's linked request; otherwise the partner would remain in MATCHED state with a dangling reference. `RequestService.cancelRequest()` / `adminCancelRequest()` locates the partner's request via `findByUserAndMatchedPartnerAndStatusAndSubject` and calls `cancel()` on it before saving.

When a **user account is deleted** (`UserService.deleteUser()`), the cascade is:  
1. All MATCHED requests pointing to the deleted user as `matchedPartner` are set to CANCELLED and their `matchedPartner` cleared.  
2. A JPQL bulk update clears any remaining FK references: `clearMatchedPartnerReferences(user)`.  
3. All requests owned by the user are deleted (`deleteByUser(user)`).  
4. The user record is deleted.

**Relevant repository method:**

```java
@Modifying
@Query("UPDATE Request r SET r.matchedPartner = null WHERE r.matchedPartner = :matchedPartner")
void clearMatchedPartnerReferences(@Param("matchedPartner") User matchedPartner);
```

This pattern ensures referential integrity across all request states without relying on database-level cascade deletes, which would be invisible to the application layer. It directly addresses Success Criteria 16 and 18.

> **[Diagram Placeholder: Criterion B § 4.5 — Cascade Cancellation Logic flowchart]**  
> **[Diagram Placeholder: Criterion B § 4.6 — User Deletion Cascade Logic flowchart]**

---

## 5. Scheduled automation and time handling

`service/RequestStatusScheduler.java` is annotated `@Scheduled(fixedDelay = 60_000)` and runs every 60 seconds (disabled in the `test` Spring profile via `@Profile("!test")`). For each MATCHED request it calls `shouldBeMarkedDone()`:

```java
boolean shouldBeMarkedDone(Request request, LocalDateTime now) {
    if (request.getWeekStartDate() == null || request.getChosenTimeslot() == null)
        return false;
    LocalDateTime endTime = Timeslots.getTimeslotEndTime(
        request.getWeekStartDate(), request.getChosenTimeslot());
    return endTime != null && now.isAfter(endTime);
}
```

`Timeslots.getTimeslotEndTime()` derives the session's end `LocalDateTime` by: parsing the day code (e.g. `TUE`) to an offset from the `weekStartDate` Monday, then reading the period end time (e.g. `10:45` for P2) from a static array. The result is compared against `TimeService.now()`, an injectable clock that allows simulated time during local testing. This design separates clock access from system time, a standard defensive-programming technique that avoids test flakiness. It directly satisfies Success Criterion 13.

> **[Diagram Placeholder: Criterion B § 4.7 — Scheduler DONE Transition Logic flowchart]**  
> **[Diagram Placeholder: Criterion B § 2.3 — Hierarchical Chart (Part 3): Scheduled Automation and Data Access Layer]**

---

## 6. Security, validation, and defensive programming

**Password hashing.** `config/PasswordConfig.java` exposes a `BCryptPasswordEncoder` bean. `UserService.registerUser()` calls `passwordEncoder.encode(rawPassword)` before persisting; the raw password is never stored. Spring Security's `DaoAuthenticationProvider` uses the same encoder for login verification, satisfying Success Criteria 1–3 and the security requirement in Criterion 19.

**Role-based access control.** `SecurityConfig.java` applies URL-level rules: `/admin/**` requires `ROLE_ADMIN`; `/dashboard`, `/profile/**`, and `/requests/**` require `ROLE_STUDENT`. Individual controller methods carry `@PreAuthorize("hasRole('ADMIN')")` where finer control is needed. `RoleRedirectAccessDeniedHandler` redirects denied students to `/dashboard` and denied admins to `/admin/dashboard` rather than showing a generic 403, as required by Success Criterion 20.

**Validation and guard clauses:**

```java
// RequestService.createRequest() — timeslot and duplicate guards
if (timeslots == null || timeslots.isEmpty())
    throw new IllegalArgumentException("At least one timeslot must be selected");

if (hasActiveRequest(user, subject, type))
    throw new IllegalArgumentException(
        "You already have an active " + typeLabel + " request for " + subject.getDisplayName());
```

Before `createRequest` is called, `RequestController` also filters the incoming timeslot strings through `Timeslots.filterValid()`, discarding any unrecognised codes. This dual-layer defence satisfies Success Criteria 8 and 9. Profile-completion gating in `DashboardController` checks `user.isProfileComplete()` before rendering the dashboard and redirects to `/profile` otherwise (Success Criterion 4).

> **[Diagram Placeholder: Criterion B § 4.1 — Authentication Workflow flowchart]**  
> **[Diagram Placeholder: Criterion B § 4.2 — Registration Workflow flowchart]**  
> **[Diagram Placeholder: Criterion B § 4.3 — Create Tutoring Request Workflow flowchart]**

---

## 7. Use of third-party libraries and justification

| Library / Framework | Role in the product | Justification |
|---|---|---|
| **Spring Boot 3** (`spring-boot-starter-web`, `spring-boot-starter-thymeleaf`) | HTTP server, MVC dispatch, Thymeleaf template engine | Eliminates boilerplate servlet configuration; auto-configuration of embedded Tomcat reduces infrastructure code and allows focus on domain logic. The production-ready starter set is the standard choice for Java web applications at this complexity level. |
| **Spring Security** (`spring-boot-starter-security`) | Authentication, authorisation, CSRF, session management | Provides a well-audited security filter chain. Implementing BCrypt login, role-based URL rules, and method-level `@PreAuthorize` from scratch would require hundreds of lines of security-critical code with a high error risk. |
| **Spring Data JPA / Hibernate** (`spring-boot-starter-data-jpa`) | ORM and repository abstraction | Maps Java entities to SQL tables; derived query methods (`findByStatus`, `existsByUserAndSubjectAndTypeAndStatus`) are generated at startup without hand-written SQL, reducing maintenance burden. The `@Modifying` JPQL query for `clearMatchedPartnerReferences` shows that custom queries are used when derived methods are insufficient. |
| **H2 / PostgreSQL** | In-memory database for development and tests; relational persistence for production | H2 allows the application to start without an external database during development; the same JPA code targets PostgreSQL in production via a datasource property switch. |
| **JGraphT** (`jgrapht-core`) | `SimpleWeightedGraph`, `MaximumWeightBipartiteMatching` | Provides a peer-reviewed implementation of the Galil-Micali-Gabow algorithm for maximum-weight bipartite matching. Implementing this algorithm from scratch would be beyond scope and error-prone; using JGraphT directly addresses the IB criterion of selecting appropriate existing tools (IB CS IA Handbook, 2023, Criterion C descriptors). |

The use of JGraphT in particular raises the technical complexity of the product appropriately: the matching problem has $O(n^2)$ edge construction and the library algorithm runs in polynomial time, which is non-trivial to reason about and would score higher on the algorithmic-thinking dimension than a simple linear scan.

---

## 8. Evidence placeholders: screenshots and reused design diagrams

The following diagrams from Criterion B (`ia_docs/crit_b_design_diagrams_new.md`) are reused as supporting evidence throughout this criterion. Physical screenshots of the running application should be attached at each placeholder when submitting the final IA.

- **[Diagram Placeholder: Criterion B § 1.1 — Domain Class Diagram (UML class diagram showing User, Request, Subject, Match relationships)]** — supports Section 2 (data model decisions).
- **[Diagram Placeholder: Criterion B § 1.2 Part 1 — Service Interaction Diagram: Controllers to Services (UML class diagram)]** — supports Section 1 (MVC architecture).
- **[Diagram Placeholder: Criterion B § 1.2 Part 2 — Service Interaction Diagram: Services to Repositories (UML class diagram)]** — supports Section 1 (persistence layer).
- **[Diagram Placeholder: Criterion B § 2.1 — Hierarchical Chart Part 1: Authentication, Profile Management, Request Lifecycle]** — supports Section 6 (security and validation).
- **[Diagram Placeholder: Criterion B § 2.2 — Hierarchical Chart Part 2: Admin Controls and Algorithmic Matching]** — supports Sections 3 and 7.
- **[Diagram Placeholder: Criterion B § 2.3 — Hierarchical Chart Part 3: Scheduled Automation and Data Access Layer]** — supports Section 5.
- **[Diagram Placeholder: Criterion B § 3.1 — Context DFD (system-level data flow, Student and Admin actors)]** — supports Section 1 (overview).
- **[Diagram Placeholder: Criterion B § 3.2 Part 1 — Level 0 DFD: Student-facing Processes (auth, profile, requests)]** — supports Sections 2 and 6.
- **[Diagram Placeholder: Criterion B § 3.2 Part 2 — Level 0 DFD: Admin-facing Processes and Background Automation]** — supports Sections 3, 4, and 5.
- **[Diagram Placeholder: Criterion B § 4.1 — Authentication Workflow flowchart]** — supports Section 6.
- **[Diagram Placeholder: Criterion B § 4.2 — Registration Workflow flowchart]** — supports Section 6.
- **[Diagram Placeholder: Criterion B § 4.3 — Create Tutoring Request Workflow flowchart]** — supports Section 6.
- **[Diagram Placeholder: Criterion B § 4.4 — Admin Matching Execution Workflow flowchart]** — supports Section 3.
- **[Diagram Placeholder: Criterion B § 4.5 — Cascade Cancellation Logic flowchart]** — supports Section 4.
- **[Diagram Placeholder: Criterion B § 4.6 — User Deletion Cascade Logic flowchart]** — supports Section 4.
- **[Diagram Placeholder: Criterion B § 4.7 — Scheduler DONE Transition Logic flowchart]** — supports Section 5.
- **[Diagram Placeholder: Criterion B § 4.8 — Dashboard Loading Logic flowchart]** — supports Section 6.
- **[Screenshot Placeholder: Student dashboard showing PENDING request with no partner visible]** — demonstrates privacy pre-match (Success Criterion 12).
- **[Screenshot Placeholder: Student dashboard showing MATCHED request with partner name and timeslot visible]** — demonstrates partner reveal post-match.
- **[Screenshot Placeholder: Admin dashboard showing match count after triggering algorithm]** — demonstrates Success Criterion 14.
- **[Screenshot Placeholder: Registration form with validation error for non-institutional email]** — demonstrates Success Criteria 2–3.

---

## Bibliography

- IB Computer Science IA Handbook. International Baccalaureate Organization, 2023. Used to identify Criterion C marking descriptors and required evidence categories.
- Spring Framework Reference Documentation. VMware, 2024. https://docs.spring.io/spring-framework/reference/ (accessed via project dependency documentation). Used for MVC architecture design and security configuration.
- JGraphT Library Documentation. JGraphT Contributors, 2024. https://jgrapht.org/ (accessed via project dependency). Used for `MaximumWeightBipartiteMatching` API and algorithm selection.
- Hibernate ORM Documentation. Red Hat, 2024. https://hibernate.org/orm/documentation/ Used for JPA mapping decisions including `FetchType`, `@ElementCollection`, and `@ManyToMany`.
- Spring Security Reference Documentation. VMware, 2024. https://docs.spring.io/spring-security/reference/ Used for `DaoAuthenticationProvider`, `BCryptPasswordEncoder`, and URL-level access rules.
