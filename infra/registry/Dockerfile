FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy full multi-module project
COPY . .

# Build ONLY registry module
RUN mvn -pl registry -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /build/registry/target/*.jar registry_service.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "registry_service.jar"]
