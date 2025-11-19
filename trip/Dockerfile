# ===========================
# BUILD STAGE
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire project (multi module)
COPY . /app

ENV PROTOC_CLASSIFIER=linux-x86_64

RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} dependency:go-offline -B
RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} -pl trip -am clean package -DskipTests

# ===========================
# RUNTIME STAGE
# ===========================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/trip/target/*.jar trip_service.jar

EXPOSE 8083 9094
ENTRYPOINT ["java", "-jar", "trip_service.jar"]
