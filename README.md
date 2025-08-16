# Bromsgrove School Student Portal

A web-based peer tutoring platform that connects students for academic support and collaborative learning.

## Overview

The Student Portal enables Bromsgrove School students to create tutoring requests (either offering or seeking help) and get matched with peers based on subjects, availability, and academic levels. The system supports role-based access with automatic role assignment based on email patterns.

## Phase 1 Features

### ✅ Completed Features

- **User Registration & Authentication**
  - School email validation (`@bromsgrove-school.co.uk`)
  - Automatic role assignment (students vs. staff based on email pattern)
  - BCrypt password encryption
  - Secure login/logout with session management

- **Profile Management**
  - Academic profile completion (year group, exam board, subjects, availability)
  - Profile completeness validation and gating
  - Automatic exam board assignment for Years 9-11 (GCSE)
  - User choice for Years 12-13 (A Levels or IB)

- **Tutoring Request System**
  - Create requests to offer tutoring (TUTOR) or seek help (TUTEE)
  - Subject selection from 8 core subjects
  - Timeslot selection (Monday-Friday, Periods 1-7)
  - Duplicate request prevention
  - Request cancellation (for PENDING requests)
  - Week start date calculation (next Monday logic)

- **Dashboard & Navigation**
  - Personalized dashboard showing user's requests
  - Request status tracking (PENDING, MATCHED, COMPLETED, CANCELLED)
  - Responsive navigation with role-based menus
  - Profile status overview

- **Security & Access Control**
  - Role-based authentication
  - Profile completion gating for dashboard access
  - CSRF protection disabled (Phase 1 - to be enabled in production)
  - Custom authentication success handlers

## Tech Stack

- **Backend**: Spring Boot 3.5.4, Java 17
- **Database**: PostgreSQL (production), H2 (testing)
- **Security**: Spring Security 6
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Build**: Maven
- **Testing**: JUnit 5, Spring Boot Test

## Local Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for development)

### Database Setup

1. Install PostgreSQL and create a database:
```sql
CREATE DATABASE studentportal;
CREATE USER studentportal WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE studentportal TO studentportal;
```

2. Set environment variables (optional):
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/studentportal
export DATABASE_USERNAME=studentportal
export DATABASE_PASSWORD=password
```

### Running the Application

1. Clone the repository:
```bash
git clone https://github.com/Geiger-J/studentportal.git
cd studentportal
```

2. Run with Maven:
```bash
mvn spring-boot:run
```

3. Access the application at: http://localhost:8080

### Testing

Run tests with H2 in-memory database:
```bash
mvn test
```

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Database (PostgreSQL for development)
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/studentportal}
spring.datasource.username=${DATABASE_USERNAME:studentportal}
spring.datasource.password=${DATABASE_PASSWORD:password}

# JPA/Hibernate (WARNING: development only!)
spring.jpa.hibernate.ddl-auto=update

# Security
spring.security.user.name=admin
spring.security.user.password=admin
```

### Environment Variables

- `DATABASE_URL`: PostgreSQL connection string
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

## User Roles & Email Patterns

The system automatically assigns roles based on email patterns:

- **Students**: Email starting with a digit (e.g., `1234@bromsgrove-school.co.uk`)
- **Staff/Admin**: Email starting with a letter (e.g., `john.smith@bromsgrove-school.co.uk`)

## Available Subjects

The system includes 8 core subjects:
- Mathematics
- Physics  
- Chemistry
- English
- Biology
- History
- Geography
- Computer Science

## Development Notes

### Database Schema Management

⚠️ **WARNING**: The current setup uses `spring.jpa.hibernate.ddl-auto=update` for development convenience. This setting:
- Should **NEVER** be used in production
- Can cause data loss
- Must be replaced with Flyway migrations before production deployment

### Architecture Decisions

- **Timeslots as Enum**: Ensures controlled vocabulary and type safety
- **Profile Completeness Logic**: Centralized in User entity with service validation
- **Week Start Calculation**: Always schedules for next Monday to allow processing time
- **Duplicate Prevention**: Enforced at service layer with database constraints
- **Simple Data Seeding**: Uses CommandLineRunner for Phase 1 (replace with Flyway later)

### Code Organization

```
src/main/java/com/example/studentportal/
├── config/          # Configuration classes (Security, Data seeding)
├── controller/      # Web controllers (MVC)
├── model/          # JPA entities and enums
├── repository/     # Data access layer
├── service/        # Business logic layer
└── util/          # Utility classes (Date calculations)
```

## Testing Strategy

- **Unit Tests**: Service layer business logic
- **Integration Tests**: Security configuration and endpoint access
- **Database Tests**: Repository functionality with H2
- **Test Profiles**: Separate configuration for testing

## Request Lifecycle

### Status Overview

The system manages tutoring requests through the following status lifecycle:

- **PENDING**: Newly created requests awaiting matching
- **MATCHED**: Successfully paired with a partner
- **NOT_MATCHED**: No suitable match found in latest matching run
- **COMPLETED**: Tutoring session completed successfully
- **CANCELLED**: Request cancelled by user before session
- **ARCHIVED**: Historical requests from previous weeks

### Weekly Matching Process

**Saturday 11:00 PM (Europe/London)**: Automated matching and archival runs

1. **Matching Phase**: Current week PENDING requests are processed
   - Matched requests → status changes to MATCHED
   - Unmatched requests → status changes to NOT_MATCHED
   - Matching algorithm determines optimal tutor-tutee pairings

2. **Archival Phase**: Previous week requests are archived
   - All requests from the previous week (regardless of status) → ARCHIVED
   - Prevents system from becoming cluttered with old requests
   - Historical data maintained for reporting and analytics

### Cancellation Rules

Users can cancel requests under specific conditions:

- **Status Requirements**: Request must be PENDING or NOT_MATCHED
- **Timing Requirements**: Current time must be at least 24 hours before the earliest session timeslot
- **Ownership**: Users can only cancel their own requests

### Admin Dashboard

Administrators have access to system-wide request management:

- **Default View**: Shows all active (non-archived) requests
- **Archived Filter**: Toggle to include archived requests in the view
- **Request Details**: Full visibility into all request information
- **Status Management**: Future features will allow status modifications

## Roadmap - Phase 2

### Planned Features

- **Matching Algorithm**: Hopcroft-Karp algorithm for optimal tutor-tutee pairing
- **Scheduling System**: Automated weekly scheduling runs
- **Status Transitions**: Automatic request lifecycle management
- **Recurring Requests**: Support for ongoing tutoring arrangements
- **Max Tutoring Enforcement**: Respect user limits for sessions per week
- **Admin Dashboard**: Staff interface for system management
- **Email Notifications**: Request status updates and reminders
- **Enhanced Security**: CSRF protection, input validation improvements
- **Database Migrations**: Flyway integration for schema versioning

### Technical Improvements

- **Performance**: Query optimization and caching
- **Monitoring**: Application metrics and health checks
- **Documentation**: API documentation with OpenAPI
- **CI/CD**: Automated testing and deployment pipeline
- **Error Handling**: Comprehensive error pages and logging

## Contributing

### Code Style

- Follow standard Java conventions
- Include JavaDoc for public methods
- Add inline comments for complex business logic
- Use descriptive variable and method names

### Testing

- Write tests for all new features
- Maintain test coverage above 80%
- Include both positive and negative test cases
- Use meaningful test method names

### Pull Requests

- Keep changes focused and minimal
- Include tests for new functionality
- Update documentation as needed
- Ensure all tests pass before submitting

## Support

For technical issues or questions:
- **Email**: support@bromsgrove-school.co.uk
- **Phone**: +44 (0)1527 579679

For academic support:
- **Email**: academic.support@bromsgrove-school.co.uk

## License

This project is proprietary software for Bromsgrove School. All rights reserved.

---

*Built with ❤️ for the Bromsgrove School community*