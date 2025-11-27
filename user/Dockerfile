# ============================
# BUILD STAGE
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# ✅ Copy FULL multi-module project
COPY . .

# ✅ Build only user module (parent is present now)
RUN mvn -pl user -am clean package -DskipTests

# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /build/user/target/*.jar user_service.jar

EXPOSE 8081 9095

ENTRYPOINT ["java", "-jar", "user_service.jar"]
