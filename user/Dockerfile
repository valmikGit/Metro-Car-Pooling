# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire source tree (multi-module build)
COPY . /app

# Build only the user module
RUN mvn -pl user -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/user/target/*.jar user_service.jar

EXPOSE 8081 9095

ENTRYPOINT ["java", "-jar", "user_service.jar"]
