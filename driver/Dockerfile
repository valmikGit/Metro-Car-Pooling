# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire multi-module Maven project FIRST
COPY . /app

# Set correct protoc classifier for Linux Docker build
ENV PROTOC_CLASSIFIER=linux-x86_64

# Go offline + build ONLY after project exists
RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} dependency:go-offline -B

# Build only the driver module (with all dependencies built automatically)
RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} -pl driver -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/driver/target/*.jar driver_service.jar

EXPOSE 8081 9090
ENTRYPOINT ["java", "-jar", "driver_service.jar"]
