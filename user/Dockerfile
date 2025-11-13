# ===========================
#  Stage 1: Build the JAR
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy the pom.xml and download dependencies (for caching)
COPY pom.xml .
COPY ../pom.xml ../

# Copy source code
COPY src ./src

# Package the application (skip tests for speed)
RUN mvn clean package -DskipTests

# ===========================
#  Stage 2: Run the app
# ===========================
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
ARG JAR_FILE=target/*.jar
COPY --from=builder /app/${JAR_FILE} user_service.jar

# Expose app port (change for each service)
EXPOSE 8080

# Default command
ENTRYPOINT ["java", "-jar", "user_service.jar"]