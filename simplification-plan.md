# Simplification Plan

This document maps the current state of the codebase and identifies potential
clutter so that a future refactor can be carried out safely and incrementally.
No code is changed here; this is a planning artefact only.

---

## 1. Current Package Structure

### Controllers (`com.example.studentportal.controller`)

| Class | Routes |
|---|---|
| `HomeController` | `GET /`, `GET /about` |
| `AuthController` | `GET /login`, `GET /register`, `POST /register` |
| `DashboardController` | `GET /dashboard` |
| `ProfileController` | `GET /profile`, `POST /profile` |
| `RequestController` | `GET /requests/new`, `POST /requests/new`, `POST /requests/{id}/cancel` |
| `AdminController` | `GET /admin/dashboard`, `GET /admin/requests`, `GET /admin/users`, `POST /admin/users/delete/{id}`, `POST /admin/match`, `POST /admin/matching/run`, `POST /admin/archive` |

### Services (`com.example.studentportal.service`)

| Class | Purpose |
|---|---|
| `UserService` | User registration, profile update, role determination, deletion |
| `RequestService` | Request creation, cancellation, querying, archiving |
| `SubjectService` | Thin wrapper: subject querying and saving |
| `MatchingService` | Bipartite graph matching algorithm (JGraphT) |
| `CustomUserDetailsService` | Spring Security `UserDetailsService` + `CustomUserPrincipal` inner class |

### Repositories (`com.example.studentportal.repository`)

| Interface | Entity |
|---|---|
| `UserRepository` | `User` |
| `RequestRepository` | `Request` |
| `SubjectRepository` | `Subject` |

### Entities / Models (`com.example.studentportal.model`)

| Class | Type | Notes |
|---|---|---|
| `User` | `@Entity` | Central entity; holds role, profile data, availability, subjects |
| `Request` | `@Entity` | Tutoring request; links two users (requester + matched partner) |
| `Subject` | `@Entity` | Subject code + display name |
| `Role` | `enum` | `STUDENT`, `ADMIN` |
| `RequestType` | `enum` | `TUTOR`, `TUTEE` |
| `RequestStatus` | `enum` | `PENDING`, `MATCHED`, `DONE`, `CANCELLED` |
| `ExamBoard` | `enum` | `NONE`, `GCSE`, `A_LEVELS`, `IB` |
| `Timeslot` | `enum` | Day/period slots for availability |

### Configuration (`com.example.studentportal.config`)

| Class | Purpose |
|---|---|
| `SecurityConfig` | Spring Security filter chain, login/logout, access rules |
| `PasswordConfig` | `@Bean` for `BCryptPasswordEncoder` |
| `CustomAuthenticationSuccessHandler` | Post-login redirect based on role / profile completeness |
| `RoleRedirectAccessDeniedHandler` | 403 → redirects ADMIN to `/admin/dashboard`, others to `/dashboard` |
| `DataSeeder` | `CommandLineRunner` that seeds subjects on first start; disabled in tests |

### Utilities (`com.example.studentportal.util`)

| Class | Purpose |
|---|---|
| `DateUtil` | Static helpers: `nextMonday`, `getMondayOfWeek`, `isMonday` |

### Templates (`src/main/resources/templates`)

```
templates/
├── index.html          (landing page)
├── login.html
├── register.html
├── dashboard.html      (student dashboard)
├── profile.html
├── request_form.html
├── about.html
├── error.html
├── admin/
│   ├── dashboard.html
│   ├── requests.html
│   └── users.html
└── fragments/
    ├── header.html
    ├── footer.html
    └── subject_selection.html
```

---

## 2. Identified Clutter Patterns

### 2a. Interfaces with only one implementation
- **Spring Security's `UserDetailsService`** → the only custom implementation in
  this project is `CustomUserDetailsService`. Spring Boot auto-detects a single
  implementation, so constructor parameters typed as `UserDetailsService` could
  accept the concrete class directly (see `AuthController`), reducing indirection.

### 2b. Pass-through services that only delegate to repositories
- **`SubjectService`** — every public method is a one-liner calling
  `SubjectRepository`:
  ```
  getAllSubjects()   → subjectRepository.findAll()
  findById(id)       → subjectRepository.findById(id)
  findByCode(code)   → subjectRepository.findByCode(code)
  save(subject)      → subjectRepository.save(subject)
  hasSubjects()      → subjectRepository.count() > 0
  ```
  Callers (DataSeeder, ProfileController, RequestController) could inject
  `SubjectRepository` directly, eliminating this class entirely.

### 2c. Duplicate matching endpoints in `AdminController`
- `POST /admin/match` and `POST /admin/matching/run` both call
  `matchingService.performMatching()` and redirect to `/admin/dashboard`.
  One of the two is dead code and can be removed after confirming no template
  uses it.

### 2d. `PasswordConfig` as a micro-configuration class
- Contains exactly one `@Bean` method.  
  The `BCryptPasswordEncoder` bean can be defined directly inside
  `SecurityConfig`, removing a separate class.

### 2e. `DateUtil` usage
- `DateUtil` provides static utility methods but is not referenced anywhere in
  the current controller/service layer (it may be left over from a planned
  feature). Verify usage before removal.

### 2f. Debug `System.out.println` in `AdminController`
- `AdminController.runMatchingAlgorithm` contains a bare `System.out.println("action in controller")`
  that should be removed or replaced with a proper `Logger` call.

### 2g. `AuthController.RegistrationForm` inner class
- A static inner class for a simple 3-field form is common but adds nesting.
  It could be promoted to a top-level DTO class, or replaced by `@RequestParam`
  parameters if Bean Validation is not strictly required.

---

## 3. Proposed Refactor Sequence

Each step is small, independently releasable, and guarded by the regression
test suite. Steps are ordered from lowest to highest risk.

| Step | Description | Risk |
|---|---|---|
| 1 | Remove `System.out.println` in `AdminController.runMatchingAlgorithm` | Trivial |
| 2 | Merge `PasswordConfig` bean into `SecurityConfig` and delete `PasswordConfig.java` | Low |
| 3 | Audit `DateUtil` references; delete if unused | Low |
| 4 | Remove the duplicate `/admin/match` endpoint (keep `/admin/matching/run`) after confirming no template links to it | Low–Medium |
| 5 | Inline `SubjectService` callers to use `SubjectRepository` directly; delete `SubjectService` | Medium |
| 6 | Replace `UserDetailsService` interface injection with `CustomUserDetailsService` concrete type where beneficial | Low |
| 7 | Promote `AuthController.RegistrationForm` to a top-level DTO class | Low |
| 8 | Evaluate extracting `CustomUserPrincipal` from `CustomUserDetailsService` into its own top-level class for clarity | Low |

**Before each step:** run `mvn test` and confirm all tests pass.  
**After each step:** commit individually so that any regression is easy to
bisect and revert.
