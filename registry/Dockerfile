FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent pom
COPY pom.xml pom.xml

# Copy registry module
COPY registry/pom.xml registry/pom.xml
COPY registry/src registry/src

# Build only registry module
RUN mvn -f registry/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/registry/target/*.jar registry_service.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "registry_service.jar"]
