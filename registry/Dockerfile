# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire multi-module Maven project
COPY . /app

# Build the registry module (with all dependencies built automatically)
RUN mvn -pl registry -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/registry/target/*.jar registry_service.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "registry_service.jar"]
