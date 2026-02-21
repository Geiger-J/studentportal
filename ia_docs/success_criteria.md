# IB Computer Science Internal Assessment — Success Criteria

> **Product:** Student Peer-Tutoring Portal  
> **Tech stack:** Java 17, Spring Boot, Spring Security, Thymeleaf, JPA/Hibernate, JGraphT  
> **Derived strictly from implemented functionality. No hypothetical features are included.**

---

## SECTION 1: FEATURE INVENTORY

### User Interface Actions

- Registration form: full name, email address, password
- Login and logout via form-based authentication
- Profile-completion form: year group, exam board, subject selection, timeslot availability selection
- New-request form: request type (offer/seek tutoring), subject, timeslots
- Student dashboard: list of own requests with current status, matched partner, and chosen timeslot
- Admin dashboard: summary counts of pending, matched, and completed requests; user statistics
- Admin request-management page: tabular list of all requests, filterable by status
- Admin user-management page: tabular list of all registered users
- Admin controls: trigger matching algorithm, cancel individual requests, archive old requests, reset user passwords, delete users

### Data Input and Validation

- Email address must end with `@example.edu`
- Password must be at least 4 characters long
- Year group must be an integer in the range 9–13 (students only)
- Students in Year 12 or 13 must select either A_LEVELS or IB as their exam board
- Profile completion requires at least one subject and at least one availability timeslot
- A new tutoring request requires at least one timeslot to be selected
- Only valid, recognised timeslot codes (e.g. `MON_P1`, `TUE_P2`) are accepted; unrecognised codes are discarded

### Data Storage and Retrieval

- User account data (name, email, BCrypt-hashed password, role, year group, exam board) persisted in database
- Many-to-many relationship between users and subjects stored in a join table
- User availability stored as a collection of timeslot codes
- Tutoring requests stored with type, subject, timeslot set, status, matched partner, chosen timeslot, and week start date
- Ten academic subjects pre-seeded into the database on first application startup
- Request status transitions (PENDING → MATCHED → DONE / CANCELLED) persisted as they occur
- Matched-partner reference and chosen timeslot written to both matched requests when a match is found
- Archived flag stored per request; archived requests are excluded from the active view

### Processing and Algorithmic Behaviour

- User role (STUDENT or ADMIN) is automatically determined from the email address: a digit-first local part indicates a student account
- Bipartite graph built at the request-timeslot level: each (request, timeslot) pair becomes a vertex
- Edges are created only for (TUTOR, TUTEE) pairs satisfying all hard constraints (same subject, shared timeslot, different users, tutor year ≥ tutee year, year difference ≤ 4)
- Each edge is assigned a weight: base 100, plus +50 for matching exam board, plus up to +30 based on year-group proximity
- A maximum-weight bipartite matching algorithm (JGraphT `MaximumWeightBipartiteMatching`) is applied, followed by a greedy pass that enforces per-user timeslot uniqueness
- Week start date for a matched session is set to the Monday of the week in which matching is run
- A scheduled task (every 60 seconds) compares each MATCHED request's timeslot end time against the current time and transitions the request to DONE when the timeslot has elapsed

### External Integrations and APIs

- No external third-party APIs are integrated in the current implementation

### Error Handling and Edge Cases

- Attempting to create a duplicate active request (same type, same subject, already PENDING) raises an error and the form is re-displayed with an explanatory message
- Students whose profiles are not yet complete are redirected to the profile page when they attempt to access tutoring features
- Spring Security's access-denied handler redirects unauthorised users to the appropriate page based on their role
- A generic error page is displayed for unhandled exceptions
- Deleting a user account automatically clears that user's `matchedPartner` references on any existing requests to prevent orphaned data

### Security and Permissions

- Passwords are hashed using BCrypt before storage; plaintext passwords are never persisted
- URL-based access control: `/admin/**` endpoints require the ADMIN role; `/dashboard` and `/requests/**` require the STUDENT role; `/login` and `/register` are publicly accessible
- Method-level `@PreAuthorize` annotations enforce role checks on controller methods
- After login, users are redirected to the admin dashboard or the student dashboard/profile page according to their role
- After registration, the new user is automatically authenticated and redirected without requiring a separate login step

---

## SECTION 2: SUCCESS CRITERIA

1. **The system allows** the user to register a new account by providing a full name, an institutional email address, and a password, and the user is automatically authenticated and redirected to their dashboard upon successful registration.

2. **The system prevents** registration with an email address that does not end in `@example.edu` and displays an informative error message on the registration form.

3. **The system prevents** registration with a password shorter than four characters and displays an informative error message on the registration form.

4. **The system redirects** a student who attempts to access the tutoring-request creation page before completing their profile to the profile-completion page, where they must provide their year group, exam board, at least one subject, and at least one available timeslot.

5. **The system restricts** the selectable exam board options to A_LEVELS and IB for students in Year 12 or 13, and displays a validation error if an incompatible combination is submitted.

6. **The system stores and retrieves** a student's selected subjects and availability timeslots so that they are pre-populated when the student views or edits their profile.

7. **The system allows** a student to create a tutoring request by selecting a request type (offer tutoring or seek tutoring), one of their profile subjects, and one or more available timeslots, and immediately displays the new request on the student dashboard.

8. **The system prevents** a student from submitting a tutoring request with no timeslots selected and displays an error message prompting the student to select at least one timeslot.

9. **The system prevents** a student from creating more than one active request of the same type for the same subject, and displays an error message if a duplicate is attempted.

10. **The system automatically** pairs TUTOR and TUTEE requests using a weighted matching algorithm that considers subject, shared timeslot, and year-group proximity, and assigns the highest-weight feasible pairing to each matched pair.

11. **The system ensures** that the matching algorithm only pairs a TUTOR with a TUTEE when they share the same subject, have at least one timeslot in common, belong to different user accounts, the tutor's year group is greater than or equal to the tutee's year group, and the year-group difference does not exceed four years.

12. **The system displays** the name of the matched partner and the chosen timeslot to a student on their dashboard once their request has been matched.

13. **The system automatically** transitions a matched request's status from "Matched" to "Done" once the scheduled session timeslot has elapsed, without requiring manual intervention.

14. **The system allows** an administrator to trigger the matching algorithm from the admin dashboard and displays the number of new matches created as a result.

15. **The system allows** an administrator to view all tutoring requests and filter the list by request status (Pending, Matched, Done, Cancelled).

16. **The system allows** an administrator to cancel any Pending or Matched request, updating the request's status to Cancelled immediately.

17. **The system allows** an administrator to archive all Done and Cancelled requests in a single action, removing them from the active request list while retaining them in the database.

18. **The system allows** an administrator to reset any user's password and to permanently delete a user account, automatically removing that user's matched-partner associations from existing requests.

19. **The system redirects** a user to the role-appropriate dashboard (admin dashboard for administrators, student dashboard or profile page for students) immediately after a successful login.

20. **The system prevents** non-administrator users from accessing any page under the `/admin` path and redirects them to an appropriate page based on their role.

---

## SECTION 3: TRACEABILITY TABLE

| Criterion | Feature | File / Module |
|-----------|---------|---------------|
| 1 | User registration with auto-authentication | `controller/AuthController.java`, `service/UserService.java` |
| 2 | Email domain validation (@example.edu) | `model/User.java`, `controller/AuthController.java` |
| 3 | Password minimum-length validation | `model/User.java` |
| 4 | Profile-completion gating on request creation | `controller/DashboardController.java`, `controller/RequestController.java`, `service/UserService.java` |
| 5 | Exam-board restriction for Year 12–13 | `controller/ProfileController.java`, `model/User.java` |
| 6 | Subject and availability persistence and retrieval | `controller/ProfileController.java`, `model/User.java`, `util/Timeslots.java` |
| 7 | Tutoring request creation | `controller/RequestController.java`, `service/RequestService.java` |
| 8 | Timeslot selection validation on request form | `controller/RequestController.java` |
| 9 | Duplicate active-request prevention | `service/RequestService.java` |
| 10 | Weighted bipartite matching algorithm | `service/MatchingService.java` |
| 11 | Matching hard constraints (subject, timeslot, year) | `service/MatchingService.java` |
| 12 | Match result display (partner, timeslot) | `templates/dashboard.html`, `model/Request.java` |
| 13 | Automatic MATCHED → DONE status transition | `service/RequestStatusScheduler.java`, `util/Timeslots.java` |
| 14 | Admin trigger-matching action | `controller/AdminController.java`, `service/MatchingService.java` |
| 15 | Admin request list with status filter | `controller/AdminController.java`, `repository/RequestRepository.java` |
| 16 | Admin request cancellation | `controller/AdminController.java`, `service/RequestService.java` |
| 17 | Admin archive of Done/Cancelled requests | `controller/AdminController.java`, `service/RequestService.java` |
| 18 | Admin user management (reset password, delete) | `controller/AdminController.java`, `service/UserService.java` |
| 19 | Role-based post-login redirect | `config/CustomAuthenticationSuccessHandler.java` |
| 20 | Admin URL access restriction | `config/SecurityConfig.java` |
