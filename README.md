# Student Portal — User Guide

A peer-tutoring coordination portal for schools.
Students can offer or seek tutoring in specific subjects and timeslots;
an admin runs the matching algorithm to pair them up.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Database setup (PostgreSQL on macOS)](#2-database-setup-postgresql-on-macos)
3. [Configuration files](#3-configuration-files)
4. [Starting the application](#4-starting-the-application)
5. [Using the portal](#5-using-the-portal)
6. [Simulation time (local testing)](#6-simulation-time-local-testing)
7. [Running the test suite](#7-running-the-test-suite)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Prerequisites

| Tool | Minimum version | Check |
|------|----------------|-------|
| Java JDK | 17 | `java -version` |
| Maven | 3.6 (or use `./mvnw`) | `mvn -version` |
| PostgreSQL | 12 (local profile) | `psql --version` |

Clone the repository:

```bash
git clone https://github.com/Geiger-J/studentportal.git
cd studentportal
```

---

## 2. Database setup (PostgreSQL on macOS)

### Install PostgreSQL via Homebrew

```bash
brew install postgresql@16
brew services start postgresql@16
```

Add the CLI tools to your PATH if needed (add to `~/.zshrc` or `~/.bash_profile`):

```bash
export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"
```

### Create the database and user

```bash
# open a postgres prompt as the system superuser
psql postgres
```

Inside the psql prompt:

```sql
CREATE DATABASE studentportal_db;
CREATE USER studentportal_user WITH PASSWORD 'studentportal_pw';
GRANT ALL PRIVILEGES ON DATABASE studentportal_db TO studentportal_user;
-- also needed on Postgres 15+:
\c studentportal_db
GRANT ALL ON SCHEMA public TO studentportal_user;
\q
```

### Verify the connection

```bash
psql -U studentportal_user -d studentportal_db -c "SELECT 1;"
```

### Stop / start PostgreSQL

```bash
brew services stop postgresql@16
brew services start postgresql@16
brew services restart postgresql@16

# if you do not want it to run automatically on login:
pg_ctl -D /opt/homebrew/var/postgresql@16 start
pg_ctl -D /opt/homebrew/var/postgresql@16 stop
```

---

## 3. Configuration files

| File | Purpose |
|------|---------|
| `src/main/resources/application.properties` | Default config — uses H2 in-memory DB (no setup needed, good for CI) |
| `src/main/resources/application-local.properties` | Local development overrides — uses PostgreSQL and enables debug logging |
| `src/test/resources/application-test.properties` | Test profile — H2 in-memory, no seeding, no scheduled jobs |

The `local` profile activates the PostgreSQL connection defined in `application-local.properties`.
You can change the password there if you chose a different one when creating the DB.

---

## 4. Starting the application

### Default mode (H2 in-memory, no external DB needed)

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080

### Local mode (PostgreSQL)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Make sure PostgreSQL is running before you start.

### Passing the simulation time on the command line (local mode)

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-Dapp.simulation.datetime=2025-01-20T09:51:00"
```

See Section 6 for details.

### Build a JAR and run it

```bash
./mvnw clean package -DskipTests
java -jar target/studentportal-0.0.1-SNAPSHOT.jar
# with the local profile:
java -jar target/studentportal-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

---

## 5. Using the portal

### Accounts and roles

| Email pattern | Role |
|--------------|------|
| Starts with a digit (e.g. `12345@example.edu`) | **Student** |
| Starts with a letter (e.g. `teacher@example.edu`) | **Admin** |

All emails must end with `@example.edu`.

The application seeds 10 standard subjects on first start
(Mathematics, Physics, Chemistry, Biology, English, German, French, Economics, Politics, Business).

### Student workflow

1. Register at `/register` with a student email.
2. Complete your profile: year group, exam board, subjects, weekly availability.
3. Create a tutoring request (`/requests/new`): choose TUTOR (offering) or TUTEE (seeking), pick a subject and timeslots.
4. Wait for an admin to run the matching algorithm.
5. Check your dashboard for a matched partner — click the envelope icon to email them directly.
6. The request status automatically changes to **Done** once the chosen timeslot has passed.

### Admin workflow

1. Register with an admin email (letter-first, e.g. `staff@example.edu`).
2. Go to `/admin/dashboard`.
3. Click **Run Matching** to pair pending tutors with tutees.
4. Use **Manage Users** to view, change passwords, or delete accounts.
5. Use **Manage Requests** to cancel any pending or matched requests.
6. Click **Archive Old Requests** to hide done/cancelled requests from the active view.

### Request lifecycle

```
PENDING --> MATCHED --> DONE   (automatic at end of scheduled timeslot)
   |            |
   +------------+--> CANCELLED  (user or admin action)
```

---

## 6. Simulation time (local testing)

The scheduler marks matched requests as **Done** when their timeslot end-time passes.
To test this without waiting for a real Monday morning, set a simulated "current time".

**Option A - via `application-local.properties`:**

```properties
# in src/main/resources/application-local.properties
app.simulation.datetime=2025-01-20T09:51:00
```

Leave blank (`app.simulation.datetime=`) to use real time.

**Option B - command-line property (one-off):**

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-Dapp.simulation.datetime=2025-01-20T09:51:00"
```

`2025-01-20` was a Monday. `09:51` is just after period P1 ends (P1 = 09:00-09:50),
so any MATCHED request with `chosenTimeslot=MON_P1` on week starting `2025-01-20`
will be marked Done within one minute of startup.

---

## 7. Running the test suite

```bash
# all tests (uses H2 in-memory, no Postgres needed)
./mvnw test

# specific test class
./mvnw test -Dtest=RequestStatusSchedulerTest

# skip tests during packaging
./mvnw clean package -DskipTests
```

Tests run against an H2 in-memory database and complete in under 30 seconds.
The background scheduler is disabled in the test profile (@Profile("!test")).

---

## 8. Troubleshooting

### Port 8080 already in use

```bash
lsof -i :8080          # find the PID
# terminate the process, then retry
# or change the port:
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### PostgreSQL connection refused

```bash
brew services list | grep postgresql   # check it is running
psql -U studentportal_user -d studentportal_db  # test credentials
```

If you need to reset the password:
```sql
ALTER USER studentportal_user WITH PASSWORD 'newpassword';
```
Then update `spring.datasource.password` in `application-local.properties` accordingly.

### H2 console (default/test mode)

Navigate to http://localhost:8080/h2-console
JDBC URL: `jdbc:h2:mem:studentportal`
Username: `sa` / Password: (blank)

### Build fails with Java version mismatch

```bash
java -version          # must be 17+
/usr/libexec/java_home -V   # macOS: list all installed JDKs
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```
