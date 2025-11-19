# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire multi-module project
COPY . /app

# Build only the matching module
RUN mvn -pl matching -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/matching/target/*.jar matching_service.jar

EXPOSE 8081 9091

ENTRYPOINT ["java", "-jar", "matching_service.jar"]
