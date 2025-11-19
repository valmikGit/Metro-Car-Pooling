# ===========================
# BUILD STAGE
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . /app

ENV PROTOC_CLASSIFIER=linux-x86_64

RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} dependency:go-offline -B
RUN mvn -Dprotoc.classifier=${PROTOC_CLASSIFIER} -pl rider -am clean package -DskipTests

# ===========================
# RUNTIME STAGE
# ===========================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/rider/target/*.jar rider_service.jar

EXPOSE 8081 9093
ENTRYPOINT ["java", "-jar", "rider_service.jar"]
