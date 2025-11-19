# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire multi-module project
COPY . /app

# Build only the notification module
RUN mvn -pl notification -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/notification/target/*.jar notification_service.jar

EXPOSE 8081 9092

ENTRYPOINT ["java", "-jar", "notification_service.jar"]
