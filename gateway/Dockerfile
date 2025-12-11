# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy full multi-module project INTO builder
COPY . .

# Build ONLY the gateway module
RUN mvn -pl gateway -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR for this module
COPY --from=builder /app/gateway/target/*.jar gateway_service.jar

# Expose HTTP port
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "gateway_service.jar"]
