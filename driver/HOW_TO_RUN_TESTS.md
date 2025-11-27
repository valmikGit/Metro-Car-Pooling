# How to Execute Driver Service Tests

I've created a comprehensive test suite for your driver service! Here's how to run them:

## 📋 Summary of Tests Created

### **Unit Tests** (3 test classes, 20+ test methods)
- `DriverServiceUnitTest` - Business logic with mocked dependencies
- `DriverCacheTest` - POJO validation
- `RedisDistributedLockUnitTest` - Lock mechanism tests

### **Integration Tests** (3 test classes, 15+ test methods)
- `DriverServiceRedisIntegrationTest` - Real Redis with Testcontainers
- `DriverServiceKafkaIntegrationTest` - Real Kafka with Testcontainers  
- `DriverGrpcServerIntegrationTest` - gRPC endpoint testing

### **Component Tests** (1 test class, 6+ end-to-end scenarios)
- `DriverServiceComponentTest` - Complete workflows

---

## 🚀 Quick Start - Execute Tests

### Option 1: Run from your existing WSL terminal

Since you already have WSL terminals open, run these commands:

```bash
# Navigate to the project
cd "Metro-Car-Pooling"

# Step 1: Install parent POM
mvn install -N -DskipTests

# Step 2: Now run tests
cd driver

# Run ONLY unit tests (fast, no Docker needed)
mvn test -Dtest="*UnitTest"

# Or run ALL tests (requires Docker running)
mvn test
```

### Option 2: Run specific test types

```bash
# Unit tests only (no Docker required, ~30 seconds)
mvn test -Dtest="*UnitTest"

# Integration tests (requires Docker, ~2-3 minutes)
mvn test -Dtest="*IntegrationTest"

# Component tests (requires Docker, ~2-3 minutes)
mvn test -Dtest="*ComponentTest"
```

### Option 3: Generate coverage report

```bash
# Run tests and generate coverage
mvn test
mvn jacoco:report

# Open the report
# File location: driver/target/site/jacoco/index.html
```

---

## 📊 What Each Test Type Does

### **Unit Tests** ✅ (No Docker Required)
- Tests business logic in isolation
- Uses Mockito for mocking Redis, Kafka, etc.
- **Fast execution** (~30 seconds)
- Tests:
  - Driver registration validation
  - Match event processing logic
  - Seat decrement logic
  - Lock acquisition/release
  - Edge cases and error handling

### **Integration Tests** 🐳 (Requires Docker)
- Tests with **real Redis and Kafka** using Testcontainers
- Validates serialization/deserialization
- Tests distributed locking with concurrency
- **Medium execution** (~2-3 minutes per test class)
- Tests:
  - Redis cache operations
  - Kafka message publishing/consuming
  - Protobuf serialization
  - gRPC client-server communication
  - Idempotency mechanisms

### **Component Tests** 🔄 (Requires Docker)
- **End-to-end workflows** combining all services
- Tests complete user journeys
- **Longer execution** (~3-5 minutes)
- Tests:
  - Full driver registration flow (gRPC → Redis)
  - Match event flow (Kafka → Redis update)
  - Concurrent operations
  - Data persistence validation

---

## 🔧 Framework & Libraries Used

- **JUnit 5** - Test framework
- **Mockito** - Mocking for unit tests
- **AssertJ** - Fluent assertions
- **Testcontainers** - Docker containers for integration tests
- **Spring Boot Test** - Application context management
- **Awaitility** - Async assertion helpers
- **JaCoCo** - Code coverage reporting

---

## 📝 Test Files Created

All test files are in `driver/src/test/java/com/metrocarpool/driver/`:

```
util/
  └── TestDataBuilder.java           # Test data factory

service/
  └── DriverServiceUnitTest.java     # Unit tests

cache/
  └── DriverCacheTest.java            # POJO tests

redislock/
  └── RedisDistributedLockUnitTest.java  # Lock tests

integration/
  ├── DriverServiceRedisIntegrationTest.java
  ├── DriverServiceKafkaIntegrationTest.java
  └── DriverGrpcServerIntegrationTest.java

component/
  └── DriverServiceComponentTest.java  # E2E tests
```

Plus configuration:
- `src/test/resources/application-test.yaml` - Test configuration

---

## ✨ Next Steps

1. **Run unit tests first** to verify basic functionality (no Docker needed)
2. **Start Docker** if you want to run integration/component tests
3. **View coverage report** to see test coverage metrics

Let me know if you encounter any issues!
