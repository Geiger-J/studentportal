# Student Portal Setup Guide

This comprehensive guide will help you set up the Bromsgrove Student Portal application from scratch.

## Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or later
- **Maven**: 3.6 or later  
- **Database**: PostgreSQL 12+ (for production) or H2 (for development/testing)
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+)

### Development Tools (Recommended)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git**: For version control
- **Docker**: Optional, for containerized PostgreSQL

## Quick Start (Development)

### 1. Clone the Repository
```bash
git clone https://github.com/Geiger-J/studentportal.git
cd studentportal
```

### 2. Run with Maven (H2 Database)
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Start the application (uses H2 in-memory database)
mvn spring-boot:run
```

### 3. Access the Application
- Open browser to: http://localhost:8080
- The application will start with H2 database and sample data

## Production Setup

### Step 1: Install Java 17

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

#### macOS
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Windows
1. Download OpenJDK 17 from https://adoptium.net/
2. Install and set JAVA_HOME environment variable
3. Add %JAVA_HOME%\bin to PATH

### Step 2: Install Maven

#### Ubuntu/Debian
```bash
sudo apt install maven
mvn -version
```

#### macOS
```bash
brew install maven
```

#### Windows
1. Download Maven from https://maven.apache.org/download.cgi
2. Extract and set M2_HOME environment variable
3. Add %M2_HOME%\bin to PATH

### Step 3: Database Setup

#### Option A: PostgreSQL (Recommended for Production)
Follow the detailed instructions in [README_DB.md](README_DB.md)

#### Option B: Docker PostgreSQL (Quick Setup)
```bash
# Start PostgreSQL container
docker run --name studentportal-postgres \
  -e POSTGRES_DB=studentportal_db \
  -e POSTGRES_USER=studentportal_user \
  -e POSTGRES_PASSWORD=secure_password_123 \
  -p 5432:5432 -d postgres:15

# Verify container is running
docker ps
```

### Step 4: Configure Application

#### Create Production Configuration
Create `src/main/resources/application-prod.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/studentportal_db
spring.datasource.username=studentportal_user
spring.datasource.password=secure_password_123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# Security Configuration
spring.security.require-ssl=false

# Logging Configuration
logging.level.com.example.studentportal=INFO
logging.level.org.springframework.security=WARN
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Scheduling Configuration (for matching algorithm)
spring.task.scheduling.pool.size=2
```

### Step 5: Build and Run

#### Development Mode
```bash
# Run with default (H2) profile
mvn spring-boot:run
```

#### Production Mode
```bash
# Clean and package
mvn clean package -DskipTests

# Run with production profile
java -jar target/studentportal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Initial Data Setup

### Admin User Creation
1. Start the application
2. Register with an admin email (must start with letters): `admin@bromsgrove-school.co.uk`
3. The system automatically assigns ADMIN role to emails starting with letters

### Student User Creation
1. Register with a student email (must start with digits): `1234@bromsgrove-school.co.uk`
2. The system automatically assigns STUDENT role to emails starting with digits

### Sample Data Seeding
The application automatically seeds:
- Academic subjects (Mathematics, English, Science, etc.)
- Timeslots (Monday P1-P4, Tuesday P1-P4, etc.)

## Configuration Options

### Email Domain Restriction
Currently restricted to `@bromsgrove-school.co.uk` domain. To change:

Edit `src/main/java/com/example/studentportal/model/User.java`:
```java
@Pattern(regexp = ".*@your-domain\\.com$", message = "Email must end with @your-domain.com")
```

### Matching Schedule
Configured in `src/main/java/com/example/studentportal/service/MatchingService.java`:
- **Weekly Matching**: Monday 6:00 AM Europe/London
- **Weekly Archival**: Saturday 11:59 PM Europe/London

To change schedule, modify the `@Scheduled` annotations:
```java
@Scheduled(cron = "0 0 6 * * MON", zone = "Europe/London")  // Weekly matching
@Scheduled(cron = "0 59 23 * * SAT", zone = "Europe/London")  // Weekly archival
```

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=RequestServiceTest
mvn test -Dtest=MatchingServiceTest
```

### Test Coverage
```bash
mvn jacoco:report
# View report in target/site/jacoco/index.html
```

## Deployment Options

### Option 1: Traditional JAR Deployment
```bash
# Build JAR
mvn clean package -DskipTests

# Copy to server
scp target/studentportal-0.0.1-SNAPSHOT.jar user@server:/opt/studentportal/

# Run on server
ssh user@server
cd /opt/studentportal
java -jar studentportal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Option 2: Docker Deployment
Create `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/studentportal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
mvn clean package -DskipTests
docker build -t studentportal .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod studentportal
```

### Option 3: System Service (Linux)
Create `/etc/systemd/system/studentportal.service`:
```ini
[Unit]
Description=Student Portal Application
After=network.target

[Service]
Type=simple
User=studentportal
ExecStart=/usr/bin/java -jar /opt/studentportal/studentportal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable studentportal
sudo systemctl start studentportal
sudo systemctl status studentportal
```

## Monitoring and Maintenance

### Application Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Log Monitoring
```bash
# If running as service
sudo journalctl -u studentportal -f

# If running manually
tail -f logs/application.log
```

### Database Maintenance
See [README_DB.md](README_DB.md) for database-specific maintenance tasks.

## Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Find process using port 8080
   lsof -i :8080
   # Kill the process or change port in application.properties
   server.port=8081
   ```

2. **Database connection error**
   - Verify PostgreSQL is running: `sudo systemctl status postgresql`
   - Check connection details in application-prod.properties
   - Ensure database and user exist

3. **Permission denied errors**
   - Check file permissions: `chmod +x mvnw`
   - Ensure Java and Maven are in PATH

4. **Memory issues**
   ```bash
   # Increase heap size
   java -Xmx2g -jar studentportal-0.0.1-SNAPSHOT.jar
   ```

### Getting Help

1. Check the application logs
2. Verify configuration files
3. Ensure all prerequisites are installed
4. Test database connectivity separately

## Security Considerations

### Production Checklist
- [ ] Change default database passwords
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up regular backups
- [ ] Enable audit logging
- [ ] Configure rate limiting
- [ ] Set up monitoring and alerting

### HTTPS Configuration
Add to `application-prod.properties`:
```properties
server.ssl.enabled=true
server.ssl.key-store-type=PKCS12
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your_keystore_password
server.ssl.key-alias=tomcat
```

## Performance Tuning

### JVM Options
```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar studentportal-0.0.1-SNAPSHOT.jar
```

### Database Connection Pool
In `application-prod.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

## Update Procedure

1. **Backup database** (see README_DB.md)
2. **Stop application**
3. **Deploy new version**
4. **Run database migrations** (automatic with spring.jpa.hibernate.ddl-auto=update)
5. **Start application**
6. **Verify functionality**