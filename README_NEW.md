# Student Portal - Enhanced Documentation

A comprehensive Spring Boot application for managing tutoring coordination within an educational institution. This portal connects students who can offer tutoring with those seeking help, featuring intelligent matching algorithms and administrative oversight.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [Architecture](#architecture)
- [User Roles and Workflows](#user-roles-and-workflows)
- [Core Functionality](#core-functionality)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

The Student Portal is a web-based application designed to facilitate peer-to-peer tutoring within a school environment. Students can register to offer tutoring (TUTOR) or seek tutoring (TUTEE) in various subjects, specifying their availability and preferences. The system includes an intelligent matching algorithm that pairs tutors with tutees based on subject, timeslot overlap, and other criteria.

### Use Cases

- **Students:** Create tutoring requests, manage availability, view matched partners, cancel requests
- **Administrators:** Oversee all requests, manage users, run matching algorithms, archive old requests
- **System:** Automatically match compatible tutoring requests, maintain data integrity, provide security

---

## Key Features

### Student Features
- ✅ **User Registration & Authentication:** Secure account creation with automatic role assignment
- ✅ **Profile Management:** Complete profiles with year group, exam board, subject preferences, and weekly availability
- ✅ **Request Creation:** Create TUTOR (offering) or TUTEE (seeking) requests for specific subjects
- ✅ **Flexible Cancellation:** Cancel requests in PENDING or MATCHED status (partner notified automatically)
- ✅ **Dashboard View:** Comprehensive view of all requests with status tracking and matched partner information
- ✅ **Duplicate Prevention:** System prevents creating multiple active requests for the same subject/type

### Administrative Features
- ✅ **Admin Dashboard:** System-wide statistics and oversight
- ✅ **User Management:** View all users, delete accounts with data cleanup
- ✅ **Request Management:** View and filter all system requests
- ✅ **Matching Algorithm:** Manually trigger intelligent matching based on subject, timeslots, and availability
- ✅ **Archive Functionality:** Archive completed (DONE) and cancelled (CANCELLED) requests to keep active views clean

### System Features
- ✅ **Intelligent Matching:** Bipartite graph-based matching using maximum weight algorithm
- ✅ **Data Integrity:** Proper foreign key management, transactional operations
- ✅ **Security:** Role-based access control, session management, CSRF protection
- ✅ **Validation:** Comprehensive input validation, business rule enforcement
- ✅ **Responsive UI:** Mobile-friendly Thymeleaf templates with modern CSS

---

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.5.4
- **Language:** Java 17
- **Security:** Spring Security 6.x
- **Data Access:** Spring Data JPA / Hibernate
- **Database:** PostgreSQL (production), H2 (development/testing)
- **Validation:** Jakarta Bean Validation

### Frontend
- **Template Engine:** Thymeleaf
- **CSS:** Custom stylesheet with responsive design
- **JavaScript:** Vanilla JS for interactive components (timeslot selection, subject cards)

### Build & Dependencies
- **Build Tool:** Maven
- **Key Libraries:**
  - JGraphT 1.5.1 (graph algorithms for matching)
  - PostgreSQL JDBC Driver
  - H2 Database (embedded)
  - Spring Boot DevTools (development)

### Development Environment
- **IDE:** VS Code, IntelliJ IDEA, or Eclipse
- **Java Version:** JDK 17+
- **Supported OS:** Windows, macOS, Linux

---

## Getting Started

### Prerequisites

Ensure you have the following installed:

1. **Java Development Kit (JDK) 17 or higher**
   ```bash
   java -version
   ```

2. **Maven 3.6+** (or use included Maven wrapper)
   ```bash
   mvn -version
   ```

3. **PostgreSQL 12+** (for production) or use H2 (for development)
   ```bash
   psql --version
   ```

4. **Git** (for cloning the repository)
   ```bash
   git --version
   ```

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/Geiger-J/studentportal.git
cd studentportal
```

#### 2. Set Up Database (PostgreSQL)

**Create Database:**
```sql
CREATE DATABASE studentportal_db;
CREATE USER studentportal_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE studentportal_db TO studentportal_user;
```

**Or use H2 (default for quick start):** No setup needed - uses in-memory database.

#### 3. Build the Project
```bash
./mvnw clean install
```

This will:
- Download all dependencies
- Compile the source code
- Run all tests
- Package the application

### Configuration

#### Application Configuration Files

The application uses Spring Boot's property files for configuration:

**Main Configuration:** `src/main/resources/application.properties`

**Default Setup (H2):**
```properties
# Database - H2 In-Memory (Development)
spring.datasource.url=jdbc:h2:mem:studentportal
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

**PostgreSQL Setup:** Uncomment and configure in `application.properties`:
```properties
# Database - PostgreSQL (Production)
spring.datasource.url=jdbc:postgresql://localhost:5432/studentportal_db
spring.datasource.username=studentportal_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**Local Profile:** For local development, use `application-local.properties` to override defaults.

#### Environment Variables

For production, use environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/studentportal_db
export SPRING_DATASOURCE_USERNAME=studentportal_user
export SPRING_DATASOURCE_PASSWORD=your_secure_password
```

### Running the Application

#### Option 1: Using Maven Wrapper (Recommended)
```bash
./mvnw spring-boot:run
```

#### Option 2: Using Maven
```bash
mvn spring-boot:run
```

#### Option 3: Run JAR file
```bash
./mvnw clean package
java -jar target/studentportal-0.0.1-SNAPSHOT.jar
```

#### Option 4: Development Mode (with auto-reload)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Access the Application:**
- Main Application: http://localhost:8080
- H2 Console (if enabled): http://localhost:8080/h2-console

**Default Port:** 8080 (change in `application.properties` with `server.port=XXXX`)

---

## Architecture

### Project Structure

```
studentportal/
├── src/
│   ├── main/
│   │   ├── java/com/example/studentportal/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── DataSeeder.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── ...
│   │   │   ├── controller/          # Web controllers (MVC)
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── ProfileController.java
│   │   │   │   └── RequestController.java
│   │   │   ├── model/               # Domain entities
│   │   │   │   ├── Request.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Subject.java
│   │   │   │   ├── RequestStatus.java (enum)
│   │   │   │   ├── RequestType.java (enum)
│   │   │   │   ├── Timeslot.java (enum)
│   │   │   │   └── Role.java (enum)
│   │   │   ├── repository/          # Data access layer
│   │   │   │   ├── RequestRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── SubjectRepository.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── RequestService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── MatchingService.java
│   │   │   │   └── SubjectService.java
│   │   │   ├── util/                # Utility classes
│   │   │   │   └── DateUtil.java
│   │   │   └── StudentportalApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── style.css
│   │       │   └── images/
│   │       ├── templates/
│   │       │   ├── admin/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── requests.html
│   │       │   │   └── users.html
│   │       │   ├── fragments/
│   │       │   │   ├── header.html
│   │       │   │   └── footer.html
│   │       │   ├── dashboard.html
│   │       │   ├── profile.html
│   │       │   ├── request_form.html
│   │       │   ├── login.html
│   │       │   ├── register.html
│   │       │   └── index.html
│   │       ├── application.properties
│   │       └── application-local.properties
│   └── test/
│       └── java/com/example/studentportal/
│           ├── service/
│           ├── security/
│           └── ...
├── pom.xml
├── README.md
├── README_NEW.md (this file)
├── TEST_PROCEDURE.md
└── mvnw / mvnw.cmd
```

### Design Patterns

- **MVC (Model-View-Controller):** Clear separation of concerns
- **Repository Pattern:** Abstraction over data access
- **Service Layer:** Business logic encapsulation
- **DTO Pattern:** (Implicit) Data transfer between layers
- **Dependency Injection:** Spring-managed beans

### Key Components

#### Models (Entities)
- **User:** Represents students and administrators with authentication, profile, and availability
- **Request:** Tutoring requests (TUTOR/TUTEE) with status lifecycle management
- **Subject:** Curated subjects organized into categories (Languages, STEM, Social Sciences)

#### Services
- **RequestService:** Request CRUD operations, cancellation logic, archiving
- **UserService:** User management, profile completion
- **MatchingService:** Intelligent bipartite matching algorithm using JGraphT
- **SubjectService:** Subject management and grouping

#### Controllers
- **AuthController:** Registration, login, logout
- **ProfileController:** Profile creation and editing
- **RequestController:** Request creation, cancellation
- **DashboardController:** Student dashboard view
- **AdminController:** Administrative functions

---

## User Roles and Workflows

### Student Role

**Registration:**
1. Register with email starting with a number (e.g., `123student@example.com`)
2. Role automatically set to STUDENT

**Profile Setup:**
1. Complete profile on first login
2. Select year group and exam board
3. Choose subjects from curated list (Languages, STEM, Social Sciences)
4. Set weekly availability using period grid (MON_P1 through FRI_P7)

**Creating Requests:**
1. Navigate to "Create Request"
2. Choose request type: TUTOR (offering) or TUTEE (seeking)
3. Select subject from your profile subjects
4. Choose timeslots from your availability
5. Submit request (status: PENDING)

**Managing Requests:**
1. View all requests on dashboard
2. See status: PENDING, MATCHED, NOT_MATCHED, DONE, CANCELLED
3. Cancel PENDING or MATCHED requests
4. View matched partner details when matched

### Admin Role

**Registration:**
1. Register with email NOT starting with a number (e.g., `admin@example.com`)
2. Role automatically set to ADMIN

**Oversight Functions:**
1. View system-wide statistics
2. Access all requests and users
3. Delete users (with data cleanup)
4. Run matching algorithm manually
5. Archive old requests (DONE/CANCELLED)

**Matching Process:**
1. Click "Run Matching Algorithm"
2. System processes all PENDING requests
3. Matches tutors with tutees based on:
   - Same subject
   - Overlapping timeslots
   - Availability compatibility
4. View match results and statistics

---

## Core Functionality

### Request Lifecycle

```
PENDING ──────────────► MATCHED ──────────► DONE
   │                        │
   │                        │
   └───► CANCELLED ◄────────┘
```

**Statuses:**
- **PENDING:** Initial state, awaiting matching
- **MATCHED:** Paired with compatible partner
- **NOT_MATCHED:** Matching attempted but no suitable match found
- **DONE:** Tutoring completed successfully
- **CANCELLED:** User-initiated cancellation (works for PENDING and MATCHED)

**Cancellation Rules:**
- PENDING requests: Only requester's request cancelled
- MATCHED requests: Both requester and partner requests set to CANCELLED
- DONE requests: Cannot be cancelled
- NOT_MATCHED requests: Cannot be cancelled (must be pending or matched)

### Matching Algorithm

The system uses a bipartite graph matching algorithm:

1. **Graph Construction:**
   - Nodes: TUTOR requests (left) and TUTEE requests (right)
   - Edges: Compatible pairs (same subject, overlapping timeslots)
   - Weights: Calculated based on timeslot overlap count

2. **Maximum Weight Matching:**
   - Uses Edmond's Blossom algorithm (via JGraphT)
   - Finds optimal pairing to maximize total compatibility

3. **Result Processing:**
   - Matched pairs: Status updated to MATCHED, partners linked
   - Unmatched requests: Remain PENDING for future matching

**Matching Criteria:**
- Same subject
- Request types match (TUTOR with TUTEE)
- At least one overlapping timeslot
- Both requests in PENDING status
- Not already matched

### Timeslot System

Students specify availability using a grid:
- **Days:** Monday through Friday
- **Periods:** P1 through P7
- **Format:** DAY_PERIOD (e.g., MON_P1, TUE_P3, FRI_P7)

Timeslots in requests must be subset of profile availability.

### Subject Categories

**Languages:**
- English
- German
- French

**STEM:**
- Mathematics
- Physics
- Biology
- Chemistry

**Social Sciences:**
- Economics
- Politics
- Business

Subjects are curated and seeded automatically on first run.

---

## Database Schema

### Core Tables

#### users
```sql
id                  BIGSERIAL PRIMARY KEY
full_name           VARCHAR(100) NOT NULL
email               VARCHAR(255) UNIQUE NOT NULL
password_hash       VARCHAR(255) NOT NULL
role                VARCHAR(20) NOT NULL
year_group          VARCHAR(20)
exam_board          VARCHAR(20)
max_tutoring_per_week INTEGER
profile_complete    BOOLEAN DEFAULT FALSE
created_at          TIMESTAMP NOT NULL
updated_at          TIMESTAMP NOT NULL
```

#### requests
```sql
id                  BIGSERIAL PRIMARY KEY
user_id             BIGINT REFERENCES users(id)
type                VARCHAR(20) NOT NULL
subject_id          BIGINT REFERENCES subjects(id)
status              VARCHAR(20) NOT NULL
chosen_timeslot     VARCHAR(20)
archived            BOOLEAN DEFAULT FALSE
matched_partner_id  BIGINT REFERENCES users(id)
created_at          TIMESTAMP NOT NULL
updated_at          TIMESTAMP NOT NULL
```

#### subjects
```sql
id                  BIGSERIAL PRIMARY KEY
code                VARCHAR(50) UNIQUE NOT NULL
display_name        VARCHAR(100) NOT NULL
```

#### request_timeslots (Many-to-Many)
```sql
request_id          BIGINT REFERENCES requests(id)
timeslot            VARCHAR(20) NOT NULL
```

#### user_availability (Many-to-Many)
```sql
user_id             BIGINT REFERENCES users(id)
timeslot            VARCHAR(20) NOT NULL
```

#### user_subjects (Many-to-Many)
```sql
user_id             BIGINT REFERENCES users(id)
subject_id          BIGINT REFERENCES subjects(id)
```

### Relationships

- User (1) → (N) Request
- Subject (1) → (N) Request
- Request (N) ↔ (N) Timeslot
- User (N) ↔ (N) Subject
- User (N) ↔ (N) Timeslot (availability)
- Request (N) → (1) User (matched partner)

---

## API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page |
| GET | `/login` | Login page |
| POST | `/login` | Process login |
| GET | `/register` | Registration page |
| POST | `/register` | Process registration |
| GET | `/logout` | Logout user |

### Student Endpoints (Requires STUDENT role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Student dashboard |
| GET | `/profile` | Profile management page |
| POST | `/profile` | Update profile |
| GET | `/requests/new` | Create request form |
| POST | `/requests` | Submit new request |
| POST | `/requests/{id}/cancel` | Cancel request |

### Admin Endpoints (Requires ADMIN role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/dashboard` | Admin dashboard |
| GET | `/admin/requests` | View all requests |
| GET | `/admin/users` | View all users |
| POST | `/admin/users/delete/{id}` | Delete user |
| POST | `/admin/matching/run` | Run matching algorithm |
| POST | `/admin/archive` | Archive old requests |

### Security

- **Authentication:** Spring Security session-based
- **Authorization:** Role-based (@PreAuthorize annotations)
- **CSRF Protection:** Enabled (tokens in forms)
- **Password Encoding:** BCrypt hashing

---

## Testing

### Run All Tests
```bash
./mvnw test
```

### Test Coverage
- Unit tests for services
- Integration tests for repositories
- Security configuration tests
- Controller tests (as needed)

### Manual Testing
Refer to [TEST_PROCEDURE.md](TEST_PROCEDURE.md) for comprehensive manual testing guide.

### Test Users
For quick testing, register:
- Student: `111student@test.com` / `Password123!`
- Admin: `admin@test.com` / `Admin123!`

---

## Deployment

### Development Deployment

**Using H2 (Default):**
```bash
./mvnw spring-boot:run
```

**Using PostgreSQL:**
1. Set up PostgreSQL database
2. Configure `application.properties`
3. Run application

### Production Deployment

#### Prerequisites
- Java 17+ runtime
- PostgreSQL database
- Reverse proxy (Nginx/Apache) recommended

#### Steps

1. **Build Production JAR:**
```bash
./mvnw clean package -DskipTests
```

2. **Set Environment Variables:**
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/studentportal_db
export SPRING_DATASOURCE_USERNAME=studentportal_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export SERVER_PORT=8080
```

3. **Run Application:**
```bash
java -jar target/studentportal-0.0.1-SNAPSHOT.jar
```

4. **Setup Systemd Service (Linux):**
Create `/etc/systemd/system/studentportal.service`:
```ini
[Unit]
Description=Student Portal Application
After=network.target

[Service]
Type=simple
User=studentportal
WorkingDirectory=/opt/studentportal
ExecStart=/usr/bin/java -jar /opt/studentportal/studentportal.jar
Restart=on-failure
Environment="SPRING_PROFILES_ACTIVE=prod"

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable studentportal
sudo systemctl start studentportal
```

#### Docker Deployment (Future)
Docker support is planned but not currently implemented due to hardware limitations.

### Database Migrations

**Current Approach:** Hibernate auto-DDL (`ddl-auto=update`)
- ⚠️ **Development only** - not recommended for production

**Planned:** Flyway migrations for production
- Version-controlled schema changes
- Rollback support
- Team synchronization

---

## Troubleshooting

### Common Issues

#### 1. Application Won't Start

**Error: Port 8080 already in use**
```bash
# Check what's using the port
lsof -i :8080
# Kill the process or change port in application.properties
server.port=8081
```

**Error: Database connection failed**
- Verify PostgreSQL is running: `systemctl status postgresql`
- Check credentials in `application.properties`
- Ensure database exists: `psql -l`

#### 2. Build Failures

**Error: Maven dependencies not downloading**
```bash
# Clear Maven cache and retry
rm -rf ~/.m2/repository
./mvnw clean install
```

**Error: Java version mismatch**
```bash
# Check Java version
java -version
# Should be 17 or higher
```

#### 3. Login Issues

**Cannot login after registration**
- Check if profile completion required
- Verify password meets requirements
- Clear browser cache/cookies

#### 4. Request Creation Failures

**Error: "At least one timeslot must be selected"**
- Ensure timeslots are selected in form
- Check JavaScript console for errors

**Error: "You already have an active request"**
- Check dashboard for existing PENDING request
- Cancel existing request if needed

#### 5. Matching Algorithm Issues

**No matches found when expected**
- Verify subject matches exactly
- Check timeslot overlap
- Ensure both requests are PENDING
- Review matching algorithm logs

### Logging

**Enable Debug Logging:**
Add to `application.properties`:
```properties
logging.level.com.example.studentportal=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

**View Logs:**
```bash
# Console output (default)
# Or configure file logging
logging.file.name=studentportal.log
```

### Database Access

**H2 Console (Development):**
1. Navigate to http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:studentportal`
3. Username: `sa`
4. Password: (empty)

**PostgreSQL:**
```bash
psql -U studentportal_user -d studentportal_db
```

### Getting Help

1. Check application logs
2. Review [TEST_PROCEDURE.md](TEST_PROCEDURE.md)
3. Search existing GitHub issues
4. Create new issue with:
   - Error message
   - Steps to reproduce
   - Environment details
   - Relevant logs

---

## Contributing

### Development Workflow

1. **Fork the repository**
2. **Create feature branch:** `git checkout -b feature/my-feature`
3. **Make changes and test:** Ensure all tests pass
4. **Commit changes:** `git commit -m "Add my feature"`
5. **Push to branch:** `git push origin feature/my-feature`
6. **Create Pull Request**

### Code Style

- Follow Java naming conventions
- Use meaningful variable/method names
- Add Javadoc comments for public methods
- Keep methods focused and concise
- Write unit tests for new features

### Testing Requirements

- All existing tests must pass
- Add tests for new functionality
- Aim for >80% code coverage
- Include integration tests where appropriate

### Pull Request Guidelines

- Clear description of changes
- Reference related issues
- Update documentation if needed
- Ensure CI/CD pipeline passes

---

## Future Enhancements

### Planned Features

- [ ] Recurring requests support
- [ ] Email notifications for matches and reminders
- [ ] Max tutoring per week enforcement
- [ ] Advanced filtering and search
- [ ] Request rating/feedback system
- [ ] Calendar integration
- [ ] Mobile application
- [ ] Automated matching schedules
- [ ] Multi-language support
- [ ] Enhanced analytics dashboard

### Technical Improvements

- [ ] Flyway database migrations
- [ ] Docker containerization
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Performance optimization
- [ ] Enhanced security hardening
- [ ] Caching layer (Redis)
- [ ] WebSocket for real-time updates

---

## License

This project is proprietary software developed for educational purposes.

---

## Acknowledgments

- Spring Boot team for excellent framework
- JGraphT for graph algorithms
- Bootstrap (if used) for UI components
- All contributors and testers

---

## Contact

For questions, issues, or contributions:
- **GitHub Issues:** https://github.com/Geiger-J/studentportal/issues
- **Repository:** https://github.com/Geiger-J/studentportal

---

## Version History

### v0.0.1-SNAPSHOT (Current)
- Initial release
- Core functionality: Registration, profiles, requests, matching
- Admin dashboard
- Request cancellation (including matched requests)
- Archive functionality
- Comprehensive testing documentation

---

**Last Updated:** November 2025

For the original README, see [README.md](README.md).
For comprehensive testing procedures, see [TEST_PROCEDURE.md](TEST_PROCEDURE.md).
