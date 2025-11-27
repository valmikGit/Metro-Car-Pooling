# Driver Service Testing Guide

## Prerequisites

- **Java 21** installed
- **Maven** installed
- **Docker** running (for integration and component tests only)

## Quick Start - Run Unit Tests Only

Unit tests are fast and don't require Docker:

```bash
# From WSL terminal
cd "/mnt/c/Users/micro/Desktop/Abhinav college/Resources/Sem 7/SPE/Final Proj"
bash run-unit-tests-only.sh
```

## Run All Tests

This runs unit tests, integration tests, and component tests (requires Docker):

```bash
# From WSL terminal
cd "/mnt/c/Users/micro/Desktop/Abhinav college/Resources/Sem 7/SPE/Final Proj"
bash run-driver-tests.sh
```

## Run Tests Manually

### 1. Build Dependencies

```bash
cd Metro-Car-Pooling

# Install parent POM
mvn install -N -DskipTests

# Install contracts module
cd contracts
mvn install -DskipTests
```

### 2. Run Unit Tests (No Docker Required)

```bash
cd ../driver
mvn test -Dtest="*UnitTest"
```

### 3. Run Integration Tests (Requires Docker)

```bash
mvn test -Dtest="*IntegrationTest"
```

### 4. Run Component Tests (Requires Docker)

```bash
mvn test -Dtest="*ComponentTest"
```

### 5. Run All Tests

```bash
mvn test
```

### 6. Generate Coverage Report

```bash
mvn jacoco:report
```

Then open: `driver/target/site/jacoco/index.html`

## Test Structure

### Unit Tests (`*UnitTest.java`)
- **DriverServiceUnitTest**: Business logic tests with mocked dependencies
- **DriverCacheTest**: POJO tests for DriverCache
- **RedisDistributedLockUnitTest**: Distributed lock logic tests

**Technologies**: JUnit 5, Mockito, AssertJ

### Integration Tests (`*IntegrationTest.java`)
- **DriverServiceRedisIntegrationTest**: Redis operations with real Redis container
- **DriverServiceKafkaIntegrationTest**: Kafka messaging with real Kafka container
- **DriverGrpcServerIntegrationTest**: gRPC endpoints with in-process server

**Technologies**: Testcontainers, Spring Boot Test

### Component Tests (`*ComponentTest.java`)
- **DriverServiceComponentTest**: End-to-end workflows combining all services

**Technologies**: Testcontainers (Kafka + Redis), Spring Boot Test

## Troubleshooting

### Issue: "Could not find artifact com.metrocarpool:contracts"
**Solution**: Build the contracts module first:
```bash
cd Metro-Car-Pooling/contracts
mvn install -DskipTests
```

### Issue: "Could not find artifact metrocarpool:pom"
**Solution**: Install parent POM first:
```bash
cd Metro-Car-Pooling
mvn install -N -DskipTests
```

### Issue: Integration tests failing with "Could not start container"
**Solution**: Make sure Docker is running:
```bash
docker ps
```

### Issue: Tests hang on Testcontainers
**Solution**: Ensure Docker has sufficient resources (4GB+ RAM recommended)

## Test Coverage

View coverage report after running tests:
```bash
firefox driver/target/site/jacoco/index.html
```

**Target Coverage**: 80%+ for business logic

## CI/CD Integration

To skip tests in builds:
```bash
mvn install -DskipTests
```

To fail build on test failures:
```bash
mvn verify
```
