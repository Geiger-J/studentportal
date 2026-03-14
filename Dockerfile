# 1. Start from a base image that has Java 21 installed
FROM eclipse-temurin:21-jre

# 2. Set a working directory inside the container
WORKDIR /app

# 3. Copy the JAR file from your machine into the container
COPY target/*.jar app.jar

# 4. When the container starts, run the app
#    We pass the 'local' Spring profile so it uses PostgreSQL
ENTRYPOINT ["java", "-jar", "app.jar"]
