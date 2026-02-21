# Student Portal — Technical Documentation

How the application is structured, how data flows through it, and how the key algorithms work.

---

## Table of Contents

1. [Architecture overview](#1-architecture-overview)
2. [Package layout](#2-package-layout)
3. [Domain model](#3-domain-model)
4. [Request lifecycle and status transitions](#4-request-lifecycle-and-status-transitions)
5. [Matching algorithm](#5-matching-algorithm)
6. [Automated "Done" transition (scheduler)](#6-automated-done-transition-scheduler)
7. [Simulation time](#7-simulation-time)
8. [User deletion — cascading cancellation](#8-user-deletion--cascading-cancellation)
9. [Security and authentication](#9-security-and-authentication)
10. [Database configuration by profile](#10-database-configuration-by-profile)
11. [Testing strategy](#11-testing-strategy)

---

## 1. Architecture overview

The application follows the classic Spring MVC layered architecture:

```
Browser
  |
  | HTTP
  v
Controllers  (translate HTTP <-> Java objects)
  |
  v
Services     (business logic — transactions live here)
  |
  v
Repositories (Spring Data JPA — talks to DB)
  |
  v
Database     (H2 for CI/tests, PostgreSQL for local/prod)
```

Templates (Thymeleaf) are rendered server-side by the controllers and returned as HTML.
There is no separate frontend build step.

---

## 2. Package layout

```
com.example.studentportal/
  config/
    DataSeeder              -- seeds subjects on first startup
    GlobalModelAdvice       -- adds label maps to every Thymeleaf model
    PasswordConfig          -- BCrypt bean
    SecurityConfig          -- route protection rules, login/logout config
    CustomAuthenticationSuccessHandler  -- redirect by role after login
    RoleRedirectAccessDeniedHandler     -- friendly 403 redirect
  controller/
    AuthController          -- /register, /login, /logout
    DashboardController     -- /dashboard (students)
    ProfileController       -- /profile, /profile/delete-account
    RequestController       -- /requests/new, /requests/{id}/cancel
    AdminController         -- /admin/** (admin-only)
    HomeController          -- / (index page)
  model/
    User                    -- JPA entity: students and admins
    Request                 -- JPA entity: tutoring requests
    Subject                 -- JPA entity: curated subject list
  repository/
    UserRepository          -- Spring Data queries for users
    RequestRepository       -- Spring Data queries for requests
    SubjectRepository       -- Spring Data queries for subjects
  service/
    UserService             -- registration, profile, password, deletion
    RequestService          -- create, cancel, archive, admin-cancel
    MatchingService         -- bipartite matching algorithm
    SubjectService          -- subject lookup helpers
    TimeService             -- returns real or simulated current time
    RequestStatusScheduler  -- @Scheduled: MATCHED -> DONE transitions
    CustomUserDetailsService -- Spring Security user loading
  util/
    Timeslots               -- all slot codes, labels, end-time calculator
    DateUtil                -- week-start date helpers
```

---

## 3. Domain model

### User

Represents both students and admins.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK, auto-increment |
| fullName | String | display name |
| email | String | unique, must end @example.edu |
| passwordHash | String | BCrypt |
| role | String | "STUDENT" or "ADMIN" |
| yearGroup | Integer | 9-13 |
| examBoard | String | "A_LEVELS", "IB", "GCSE", "NONE" |
| subjects | Set<Subject> | many-to-many via user_subjects |
| availability | Set<String> | slot codes, e.g. "MON_P1" |
| profileComplete | Boolean | computed flag |

Role is determined at registration from the email: if the local part (before @) starts with a digit -> STUDENT, otherwise -> ADMIN.

### Subject

A curated subject entry. Seeded once at startup by DataSeeder.

| Field | Type |
|-------|------|
| id | Long |
| code | String (unique) |
| displayName | String |

### Request

A tutoring request created by a student.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| user | User | owner |
| type | String | "TUTOR" or "TUTEE" |
| subject | Subject | |
| timeslots | Set<String> | available slots e.g. {"MON_P1","TUE_P3"} |
| chosenTimeslot | String | single slot selected by matching algorithm |
| weekStartDate | LocalDate | Monday of the session's week (set at match time) |
| status | String | PENDING / MATCHED / DONE / CANCELLED |
| matchedPartner | User | other side of the match (null if PENDING) |
| archived | Boolean | soft-hide from active views |
| createdAt | LocalDateTime | set automatically by Hibernate |

---

## 4. Request lifecycle and status transitions

```
[Created] --> PENDING
                |
    Admin runs matching
                |
           MATCHED  <-- chosenTimeslot and weekStartDate set here
           /      \
  Slot time passes  User or admin cancels
          |                |
         DONE          CANCELLED
```

### Rules enforced in code

- **Duplicate prevention:** a user cannot have two PENDING requests of the same type for the same subject (`RequestService.hasActiveRequest`).
- **Cancel cascade:** cancelling a MATCHED request also cancels the partner's request (`RequestService.cancelRequest` and `adminCancelRequest`).
- **Done transition:** handled automatically by `RequestStatusScheduler` — no manual step needed.
- **User deletion cascade:** when a user is deleted, any other user whose request was MATCHED to the deleted user has their request set to CANCELLED. The deleted user's own requests are removed from the database.

---

## 5. Matching algorithm

The algorithm is in `MatchingService.runMatching()` and uses a **maximum-weight bipartite matching** via the JGraphT library.

### Step-by-step

1. **Load candidates:** fetch all PENDING TUTOR requests and all PENDING TUTEE requests.

2. **Build vertices:** for every (request, timeslot) combination, create a `RequestTimeslot` node.
   - TUTOR nodes form the left partition.
   - TUTEE nodes form the right partition.

3. **Add edges:** connect TUTOR node to TUTEE node if they pass hard constraints (same subject, same timeslot, tutor year >= tutee year, different users). Assign an edge weight.

4. **Weight formula:**
   - Base: 100 points.
   - Same exam board: +50 points.
   - Year gap bonus: +30 (gap=1), +25 (gap=0), +20 (gap=2), +15 (gap=3), +10 (gap=4).

5. **Run `MaximumWeightBipartiteMatching`** — JGraphT finds the set of edges that maximises total weight while ensuring each request is matched at most once.

6. **Apply results:** for each matched edge, set both requests to MATCHED, assign `matchedPartner`, `chosenTimeslot`, and `weekStartDate` (Monday of the current week from `TimeService.today()`).

### Why bipartite matching?

A naive greedy approach (match the best pair first) can leave later requests unmatched even though a globally better assignment exists. The bipartite algorithm finds the globally optimal solution.

---

## 6. Automated "Done" transition (scheduler)

`RequestStatusScheduler.markCompletedRequestsDone()` runs every 60 seconds (disabled in the test profile).

### Logic

For each MATCHED request:
1. Skip if `weekStartDate` or `chosenTimeslot` is null (pre-existing data without these fields).
2. Compute end time: `Timeslots.getTimeslotEndTime(weekStartDate, chosenTimeslot)`.
3. If `now()` (from `TimeService`) is strictly after the end time, set status to DONE and save.

### Timeslot end times

| Period | Time range |
|--------|-----------|
| P1 | 09:00 - 09:50 |
| P2 | 09:55 - 10:45 |
| P3 | 11:05 - 11:55 |
| P4 | 12:00 - 12:50 |
| P5 | 14:05 - 14:55 |
| P6 | 15:00 - 15:50 |
| P7 | 16:00 - 17:15 |

Example: a request with `weekStartDate=2025-01-20` (Mon) and `chosenTimeslot=TUE_P2`
becomes DONE after 10:45 on Tuesday 21 Jan 2025.

Both the TUTOR and the TUTEE request for a session are checked independently,
so both will be marked Done within the same scheduler tick.

---

## 7. Simulation time

All "current time" reads in the application go through `TimeService.now()`.

```java
// real time:
return LocalDateTime.now();

// simulated time (if app.simulation.datetime is set in properties):
return LocalDateTime.parse(simulationDatetime);
```

This means you can freeze the application's clock at any point in time by setting:

```properties
app.simulation.datetime=2025-01-20T09:51:00
```

The scheduler will immediately see requests as Done if their end time is before this value.
The matching algorithm will assign `weekStartDate` based on this simulated date.

---

## 8. User deletion — cascading cancellation

`UserService.deleteUser(id)` runs these steps in order, all within one transaction:

1. **Cancel partners:** find all MATCHED requests from other users where `matchedPartner == deletedUser`.
   Set those requests to CANCELLED and clear their `matchedPartner` field.
   The partners will see CANCELLED on their dashboard, not a broken/missing state.

2. **Clear remaining references:** a JPQL bulk update sets `matchedPartner = null` for any
   remaining request that still points to the deleted user (e.g. DONE requests).

3. **Delete the user's own requests:** all requests owned by the deleted user are removed.

4. **Delete the user:** Hibernate cascades to `user_subjects` and `user_availability` join tables.

---

## 9. Security and authentication

Spring Security handles all authentication and authorisation.

### Login flow

1. User submits `/login` form with email + password.
2. `CustomUserDetailsService.loadUserByUsername()` loads the `User` entity by email,
   wraps it in a `CustomUserPrincipal` that implements `UserDetails`.
3. Spring Security compares the submitted password against the BCrypt hash.
4. On success, `CustomAuthenticationSuccessHandler` redirects:
   - ADMIN -> `/admin/dashboard`
   - STUDENT with complete profile -> `/dashboard`
   - STUDENT with incomplete profile -> `/profile`

### Route protection

Configured in `SecurityConfig`:

- Public: `/`, `/login`, `/register`, `/css/**`
- STUDENT only: `/dashboard`, `/profile`, `/requests/**`
- ADMIN only: `/admin/**`
- CSRF protection is enabled; all POST forms include the hidden `_csrf` token via Thymeleaf.

### Password storage

BCrypt via Spring Security's `BCryptPasswordEncoder` (bean in `PasswordConfig`).
Passwords are never stored in plain text.

---

## 10. Database configuration by profile

| Profile | Database | How to activate |
|---------|----------|----------------|
| (default) | H2 in-memory | just run `./mvnw spring-boot:run` |
| `local` | PostgreSQL | `-Dspring-boot.run.profiles=local` |
| `test` | H2 in-memory (fresh for each test run) | applied automatically by `@ActiveProfiles("test")` |

Hibernate `ddl-auto=update` is used in all non-test profiles, meaning the schema is created/adjusted on startup. Tests use `create-drop` for a clean slate each run.

---

## 11. Testing strategy

Tests are in `src/test/java/` and use `@SpringBootTest` with the `test` profile.

| Test class | What it covers |
|-----------|---------------|
| `UserServiceTest` | Registration, role detection, password hashing, deleteUser cascade |
| `RequestServiceTest` | Create, cancel, duplicate prevention, archive |
| `MatchingServiceTest` | Basic TUTOR/TUTEE pairing |
| `MatchingServiceDuplicateTest` | Already-matched requests are not re-matched |
| `MatchingServiceRepeatedRunTest` | Running matching twice is idempotent |
| `MatchingServiceTimeslotConflictTest` | Users with conflicting slots are not matched twice |
| `TimeServiceTest` | Real-time and simulation-time reading |
| `RequestStatusSchedulerTest` | shouldBeMarkedDone() logic, Timeslots.getTimeslotEndTime() |
| `SecurityConfigTest` | Route access control |

All tests run with H2 in-memory and complete without an internet connection or a running PostgreSQL instance.
