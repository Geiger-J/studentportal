# PostgreSQL Database Setup for Student Portal

This document provides comprehensive guidance for setting up and configuring PostgreSQL for the Bromsgrove Student Portal application.

## Prerequisites

- PostgreSQL 12 or later
- Java 17
- Maven 3.6+

## Database Installation

### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

### macOS (using Homebrew)
```bash
brew install postgresql
brew services start postgresql
```

### Windows
Download and install PostgreSQL from the official website: https://www.postgresql.org/download/windows/

## Database Setup

### 1. Create Database User
```sql
-- Connect as postgres superuser
sudo -u postgres psql

-- Create application user
CREATE USER studentportal_user WITH PASSWORD 'your_secure_password_here';

-- Grant connection privileges
GRANT CONNECT ON DATABASE postgres TO studentportal_user;
```

### 2. Create Application Database
```sql
-- Create the application database
CREATE DATABASE studentportal_db OWNER studentportal_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE studentportal_db TO studentportal_user;

-- Switch to the application database
\c studentportal_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO studentportal_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO studentportal_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO studentportal_user;
```

### 3. Configure Connection Settings

Create `application-prod.properties` in `src/main/resources/`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/studentportal_db
spring.datasource.username=studentportal_user
spring.datasource.password=your_secure_password_here
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000

# Performance Settings
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

## Database Schema

The application uses the following main entities:

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    year_group INTEGER,
    exam_board VARCHAR(20),
    max_tutoring_per_week INTEGER DEFAULT 0,
    profile_complete BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Requests Table
```sql
CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    recurring BOOLEAN DEFAULT FALSE,
    week_start_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    matched_partner_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Subjects Table
```sql
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL
);
```

## Backup and Maintenance

### Create Backup
```bash
pg_dump -U studentportal_user -h localhost studentportal_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restore Backup
```bash
psql -U studentportal_user -h localhost studentportal_db < backup_file.sql
```

### Database Maintenance
```sql
-- Analyze tables for query optimization
ANALYZE;

-- Vacuum to reclaim storage
VACUUM;

-- Full vacuum (requires exclusive lock)
VACUUM FULL;
```

## Production Considerations

### Security
1. Use strong passwords for database users
2. Configure PostgreSQL to only accept connections from application servers
3. Enable SSL for database connections
4. Regularly update PostgreSQL to latest version

### Performance Monitoring
1. Monitor connection pool usage
2. Watch for slow queries using `log_min_duration_statement`
3. Set up monitoring for disk space and memory usage
4. Configure appropriate shared_buffers and work_mem settings

### Connection Configuration
Add to `postgresql.conf`:
```
# Connection settings
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB

# Logging
log_statement = 'mod'
log_min_duration_statement = 1000
log_line_prefix = '%t [%p-%l] %q%u@%d '
```

## Troubleshooting

### Common Issues

1. **Connection refused**: Check if PostgreSQL is running: `sudo systemctl status postgresql`
2. **Authentication failed**: Verify username/password in application.properties
3. **Database does not exist**: Ensure database was created with correct name
4. **Permission denied**: Check user privileges on database and schema

### Useful Commands
```sql
-- Check current connections
SELECT pid, usename, application_name, client_addr, state 
FROM pg_stat_activity 
WHERE datname = 'studentportal_db';

-- View table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

## Environment Variables

For production deployment, use environment variables instead of hardcoded values:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=studentportal_db
export DB_USERNAME=studentportal_user
export DB_PASSWORD=your_secure_password_here
```

Update `application-prod.properties`:
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:studentportal_db}
spring.datasource.username=${DB_USERNAME:studentportal_user}
spring.datasource.password=${DB_PASSWORD}
```