# Student Portal — Comprehensive Project Prompt

This document describes the project in precise, implementation-level detail to guide development, testing, and review.

## 1. Purpose and Scope

- A school tutoring coordination portal where students:
  - Register and log in.
  - Complete their profile (year group, subjects, availability).
  - Create tutoring requests either to offer tutoring (TUTOR) or to seek tutoring (TUTEE).
  - See request status and optionally cancel pending requests.

- Staff/Admin roles to oversee students activity.

## 2. Tech Stack

Runtime and libraries:
- Language: Java (JDK 17+)
- Spring Boot 3.x
- Spring Security
- Thymeleaf HTML templates
- Persistence:
  - Current development profile: JPA/Hibernate with ddl-auto=update for convenience
  - Database: PostgreSQL
- Front-end:
  - Thymeleaf templates under src/main/resources/templates
  - CSS under src/main/resources/static/css/style.css
  - Light JavaScript for interactive selections (timeslots, subject card selection)
- Architecture:
  - config/ (Security, data seeding, etc.)
  - controller/ (web controllers)
  - model/ (entities, enums)
  - repository/ (Spring Data repositories)
  - service/ (business logic)
  - util/ (date/time helpers, etc.)

Development environment (hardware/software):
- Local development in VS Code on macOS (per user environment).
- Java extension for VS Code, local JDK.
- Docker in planning, unfortunately not supported for my hardware (MacOS 12.x)

Build tooling:
- Standart Maven Spring Boot application.

## 3. Core Domain Model

Entities and key fields:
- User
  - id
  - fullName
  - email
  - passwordHash
  - role (VARCHAR(20) in DB; roles include student and admin)
  - yearGroup
  - examBoard
  - maxTutoringPerWeek (to be implemented)
  - profileComplete (boolean)
  - createdAt, updatedAt
  - Availability (collection of Timeslot enum values, held in memory and persisted per implementation)
  - Subject preferences (curated, grouped into fields of study, eg Languages)

- Subject
  - id
  - code
  - displayName
  - Group/category (Languages, STEM, Social Sciences) used for nice presentation

- Request
  - id
  - user (owner who created it)
  - type (RequestType enum: TUTOR or TUTEE)
  - subject (Subject)
  - timeslots (collection of enum values like MON_P1, TUE_P2, etc.)
  - weekStartDate (the Monday of the week the request targets)
  - status (RequestStatus enum)
  - matchedPartner (User; set after matching)
  - createdAt, updatedAt
  - archived (boolean)
  - Note: A recurring boolean appears in the draft DB schema but is not yet part of the current working entity/UI logic.

Enums:
- RequestType
  - TUTOR ("Offering Tutoring")
  - TUTEE ("Seeking Tutoring")

- RequestStatus (lifecycle; see section 6)
  - PENDING
  - MATCHED
  - NOT_MATCHED
  - DONE
  - CANCELLED
  - ARCHIVED: is handled by a separate boolean field.

- Timeslot (naming pattern as used in views and forms)
  - Values like MON_P1, TUE_P1, WED_P1, THU_P1, FRI_P1, repeated across P1–P7 periods.
  - Saturday timeslots to be considered.

## 4. Curated Subjects and Grouping

Curated subjects, grouped for UI selection:

- Languages:
  - English
  - German
  - French

- STEM:
  - Mathematics
  - Physics
  - Biology
  - Chemistry

- Social Sciences:
  - Economics
  - Politics
  - Business

These groups are used in:
- Profile completion: selecting subjects of interest/competency.
- Request creation: selecting a single subject for the new request using the same curated grouping UI.

This list is to be extended. A consideration might be to add IB, A-Level or GCSE exclusive Subjects. The necessity is not given though.

## 5. Key Pages and UX Flows

(A) Registration and Login
- Users can register with name, email, and password.
- If the email starts with a number, the role STUDENT is assigned. If not, the role ADMIN is.
- On first login, STUDENTS ONLY are prompted to complete their profile.

(B) Profile Completion (/profile)
- Year group selection and other academic details.
- Availability selection using a table-based UI:
  - Columns: Monday to Friday
  - Rows: Periods P1 - P7
  - Each cell corresponds to a Timeslot enum (e.g. MON_P2).
  - Clicking a cell toggles selection, visually indicated by CSS (.timeslot-cell.selected).
- Curated subject selection:
  - Grouped by Languages/STEM/Social Sciences.
  - Card-based UI; selecting a subject toggles a selected visual state and stores the value in the form submission.

(C) Create Request (/requests/new)
- Choose RequestType (TUTOR or TUTEE).
- Choose Subject via the same curated grouped UI used in profile.
- Timeslot selection:
  - Same table-based UI.
  - Cells not present in the user’s availability are disabled and display a tooltip suggesting editing the profile to enable those timeslots.
- Week Start Date is shown (defaults to the next Monday).
- Submit creates the request and redirects to the dashboard.

(D) Dashboard (/dashboard)
- Lists the user’s requests in a table with the following columns:
  - Subject (request.subject.displayName)
  - Type (RequestType display name, CSS tagged type-tutor or type-tutee)
  - Timeslots (count + inline labels for each selected timeslot)
  - Status (RequestStatus with a CSS badge using the lowercase status as a class suffix)
  - Matched Partner (matched partner’s full name if present; “Not matched” otherwise)
  - Created (createdAt)
  - Week Start (weekStartDate)
  - Actions (e.g., Cancel when allowed)
- Cancel action:
  - POST to /requests/{id}/cancel
  - Confirm dialog in UI
  - Backend validation ensures only the owner can cancel and only when cancellable.

## 6. Request Lifecycle (Statuses and Transitions)

Statuses (from RequestStatus enum) define the lifecycle:

- PENDING
  - Initial status upon request creation.
  - Request is visible on the dashboard; cancel action may be available.
- MATCHED
  - Set when a tutoring match is established (future matching algorithm; currently planned).
  - Shows matched partner on the dashboard.
- NOT_MATCHED
  - Indicates that no match could be found during a matching cycle.
  - Still visible for user action or reprocessing.
- DONE
  - Indicates the tutoring session/arrangement was completed successfully.
- CANCELLED
  - User-initiated or system-initiated cancellation.
  - Requests can only be cancelled if Request.canBeCancelled() returns true (business logic enforced in the entity/service).

Archiving:
- ARCHIVED is not a status; repository/service expose queries using archived=false to list active/non-archived requests.
- Archiving may be used to hide historical/completed requests from primary views.

Notes:
- Matching algorithm, automated transitions, and recurring handling are planned enhancements.
- The service layer exposes methods such as:
  - hasActiveRequest(user, subject, type): prevent duplicates while pending.
  - getUserRequests(user): fetch user’s requests ordered by createdAt desc.
  - cancelRequest(id, user): authorization and transition to CANCELLED when allowed.

## 7. Security and Roles

- Spring Security guards routes based on authentication and role (role stored in users.role).
- Roles:
  - The database schema includes a role column (VARCHAR(20)); typical roles are Student and Admin/Staff.
  - Role-specific capabilities:
    - Student: registration, profile management, create/cancel their own requests, view their dashboard.
    - Admin/Staff (planned UI): oversight of requests, matching, system management dashboards.
- Login Emails by Role:
  - The application supports distinct roles; specific seeded or test email accounts (if any) depend on the repository’s data seeding configuration. Use the configured seed data or create accounts reflecting:
    - Student role (e.g., student@example.edu)
    - Admin/Staff role (e.g., admin@example.edu)
  - Exact addresses and credentials should be taken from the environment-specific seeding or test fixtures in your deployment.

## 8. Styling and UI Conventions

- CSS classes for request type chips:
  - .type-tutor (Offering Tutoring) and .type-tutee (Seeking Tutoring)
- Status badge styles:
  - status-badge with class suffix matching lowercase status name (e.g., .status-badge.pending)
- Timeslot table:
  - .timeslot-table for grid styling
  - .timeslot-cell toggles .selected
  - Disabled cells use .timeslot-cell.disabled and show a tooltip
- Subject cards:
  - Group headings and a responsive card grid for each category
  - Selected subject highlights and binds to a hidden form input (or radio) for server submission

## 9. Data Persistence and Database

- Current dev approach:
  - Hibernate auto DDL (spring.jpa.hibernate.ddl-auto=update) for convenience during iteration (not recommended for production).
- Target database: PostgreSQL
  - Example configuration (see repository’s README_DB):
    - spring.datasource.url=jdbc:postgresql://localhost:5432/studentportal_db
    - spring.datasource.username=studentportal_user
    - spring.datasource.password=…
    - spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
- Schema (from README_DB) illustrates the intended shape:
  - users(id, full_name, email, password_hash, role, year_group, exam_board, max_tutoring_per_week, profile_complete, created_at, updated_at)
  - requests(id, user_id, type, subject_id, recurring, week_start_date, status, matched_partner_id, created_at, updated_at)
  - subjects(id, code, display_name)
- Migrations:
  - Planned adoption of Flyway for schema versioning before production.
  - For test and local development, H2 and/or PostgreSQL can be used per profile.

## 10) Endpoints and Routing (Representative)

- GET /dashboard
  - Requires login; shows user’s requests.
- GET /profile
  - Render profile with availability and subject selections.
- POST /profile
  - Save profile changes.
- GET /requests/new
  - Render request creation form (type, subject, timeslots).
- POST /requests
  - Create new request; server-side checks prevent duplicates (pending, same subject/type).
- POST /requests/{id}/cancel
  - Cancel the request if authorized and cancellable.

Note: Route names reflect template usage and typical Spring MVC patterns evident in the codebase.

## 11) Business Rules (Highlights)

- Users can only cancel their own requests.
- Requests can only be cancelled when cancellable per business logic (e.g., likely while PENDING).
- Duplicate pending requests for same subject and type are prevented.
- Timeslots in the request form are limited to those selected in profile availability.
- Week start date aligns to the next Monday to provide scheduling runway.

## 12) Known and Planned Features

- Planned (from repository roadmap notes):
  - Matching algorithm (e.g., Hopcroft–Karp) for optimal pairing.
  - Automated scheduling runs, status transitions.
  - Recurring requests support.
  - Max tutoring per week enforcement.
  - Admin dashboard for staff.
  - Email notifications and reminders.
  - Enhanced security hardening and CSRF configuration.
  - Flyway migrations and CI/CD pipeline.

## 13) Development Workflow

- Local development:
  - VS Code with Java support, JDK 17+, and Spring Boot tooling.
  - Run application via your build tool’s Spring Boot run task.
  - For database:
    - Initial iteration with JPA auto DDL.
    - Transition to PostgreSQL locally (Docker or native install) as needed.
- Testing:
  - Unit and integration tests per standard Spring testing setup (repositories/services/controllers).
  - H2/Hikari configuration per test profile.

## 14) Acceptance Criteria Summary

- Students can:
  - Register, log in, and complete a profile with year group, subjects (curated groups), and availability.
  - Create tutoring requests by choosing type, subject, and allowed timeslots.
  - View their requests in a sortable/readable dashboard table with status badges and matched partner (when set).
  - Cancel pending requests when permitted.

- Roles and security:
  - Role stored on user; route protection via Spring Security.
  - Admin/staff oversight functions: planned.

- Data:
  - Request lifecycle adheres to PENDING → MATCHED/NOT_MATCHED → DONE or CANCELLED.
  - Archiving supported via boolean field (non-archived shown by default queries).
  - Subject and timeslot selections persist and round-trip through UI.

## 15) Glossary

- Timeslot: A discrete scheduling unit, identified by day and period (e.g., MON_P2). Presented in a Monday–Friday by P1–Pn table.
- Request: A student’s offer to tutor or ask for tutoring, bound to a subject and timeslot set for a given week.
- Matched Partner: The counterpart user paired for a request when matched.
- Week Start Date: The Monday that anchors the scheduling week for a request.
- Curated Subjects: A school-defined list grouped into Languages, STEM, and Social Sciences.

## 16) Notes on Exactness and Source

- Status and type enums are taken directly from repository code.
- Curated subject lists and grouping are taken from repository documentation.
- UI behavior is described from the current Thymeleaf templates and CSS.
- Role information is drawn from schema; concrete seeded logins should be verified against the environment's seeding configuration.
- Library versions are sourced from runtime stack traces.
