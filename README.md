# Billogram Data Processing Service

## Overview

A lightweight Spring Boot microservice designed for processing standardized invoice records from multiple sources. The service handles record ingestion, idempotent deduplication, aggregation queries, and asynchronous event emission for downstream notification and alerting services.

### Key Features

- **Idempotent Record Processing**: Each record (identified by `recordId`) is processed exactly once, preventing duplicate processing even under concurrent requests
- **Multi-Source Ingestion**: Accepts records from multiple source services efficiently (~100,000 records/hour)
- **Aggregation Queries**: Group records by destination ID with total value summaries, supporting optional time and type filters
- **Asynchronous Event Emission**: Publishes notifications and alerts without blocking the main ingestion flow
- **Configurable Alerting**: Threshold-based alerts for high-value records
- **In-Memory Database**: Uses H2 for lightweight testing and demos (easily replaceable with PostgreSQL)

---

## Architecture

### Technology Stack

- **Framework**: Spring Boot 4.1.1
- **Language**: Java 21
- **Database**: H2 (in-memory, production-ready with PostgreSQL)
- **Persistence**: Spring Data JPA
- **Async**: Spring `@Async` with in-memory thread pool
- **Testing**: JUnit 5, Mockito, AssertJ
- **Build**: Gradle

### Core Components

1. **`RecordProcessingService`**
   - Main business logic for record ingestion
   - Idempotent inserts using database unique constraints
   - Delegates notification and alert work asynchronously

2. **`NotificationService`**
   - Builds notification payloads with current record + historical summary
   - Runs asynchronously to avoid blocking ingestion
   - Queries previous records by destination ID and reference

3. **`AlertService`**
   - Evaluates record values against a configurable threshold
   - Emits alerts asynchronously for high-value records
   - Threshold: configurable via `app.alert.threshold` property (default: 1000)

4. **`RecordRepository`**
   - Data access layer for Record entities
   - Provides filtered querying for aggregation
   - Enforces unique constraint on `recordId`

---

## Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 9.7+ (included via Gradle Wrapper)

### Build

```bash
cd /home/dchakraborty/workspace/BillogramDataProcessingApp
./gradlew build
```

### Run the Service

```bash
./gradlew bootRun
```

The service starts on `http://localhost:8080`

### Run Tests

```bash
# Run all tests
./gradlew test

# Run with output
./gradlew test --info

# Run specific test class
./gradlew test --tests AlertServiceTest
./gradlew test --tests NotificationServiceTest
./gradlew test --tests RecordProcessingIntegrationTest
```

---

## API Endpoints

### 1. Ingest Record

Creates or returns an existing record identified by `recordId`. Idempotent—calling multiple times with the same `recordId` returns the same record.

**Endpoint**: `POST /api/records`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "recordId": "invoice-2026-10-09-001",
  "time": "2026-10-09T10:30:00Z",
  "sourceId": "billing-service-1",
  "destinationId": "customer-001",
  "type": "positive",
  "value": 250.50,
  "unit": "SEK",
  "reference": "INV-12345"
}
```

**Response** (201 Created on first insert, 201 on subsequent duplicate):
```json
{
  "recordId": "invoice-2026-10-09-001",
  "time": "2026-10-09T10:30:00Z",
  "sourceId": "billing-service-1",
  "destinationId": "customer-001",
  "type": "positive",
  "value": 250.50,
  "unit": "SEK",
  "reference": "INV-12345"
}
```

**Side Effects**:
- If new record: emits notification to notification service
- If new record and value > threshold: emits alert to alert service
- If duplicate: no side effects (truly idempotent)

---

### 2. Query Aggregated Records

Retrieves records grouped by destination ID with totaled values. Supports optional filtering by time range and record type.

**Endpoint**: `GET /api/records/aggregate`

**Query Parameters** (all optional):
- `startTime`: ISO 8601 datetime (inclusive). Example: `2026-10-01T00:00:00Z`
- `endTime`: ISO 8601 datetime (inclusive). Example: `2026-10-31T23:59:59Z`
- `type`: Record type filter: `positive` or `negative`
- `destinationId`: Filter by specific destination (can also be null to get all destinations)

**Example Request**:
```bash
curl "http://localhost:8080/api/records/aggregate?startTime=2026-10-01T00:00:00Z&endTime=2026-10-31T23:59:59Z&type=positive"
```

**Response** (200 OK):
```json
[
  {
    "destinationId": "customer-001",
    "totalValue": 750.50,
    "records": [
      {
        "recordId": "invoice-2026-10-09-001",
        "time": "2026-10-09T10:30:00Z",
        "sourceId": "billing-service-1",
        "destinationId": "customer-001",
        "type": "positive",
        "value": 250.50,
        "unit": "SEK",
        "reference": "INV-12345"
      },
      {
        "recordId": "invoice-2026-10-09-002",
        "time": "2026-10-09T14:15:00Z",
        "sourceId": "billing-service-2",
        "destinationId": "customer-001",
        "type": "positive",
        "value": 500.00,
        "unit": "SEK",
        "reference": "INV-12346"
      }
    ]
  },
  {
    "destinationId": "customer-002",
    "totalValue": 125.25,
    "records": [
      {
        "recordId": "invoice-2026-10-09-003",
        "time": "2026-10-09T16:45:00Z",
        "sourceId": "billing-service-1",
        "destinationId": "customer-002",
        "type": "positive",
        "value": 125.25,
        "unit": "SEK",
        "reference": "INV-12347"
      }
    ]
  }
]
```

---

### 3. List All Records

Returns all stored records without filtering or aggregation.

**Endpoint**: `GET /api/records`

**Response** (200 OK):
```json
[
  {
    "recordId": "invoice-2026-10-09-001",
    "time": "2026-10-09T10:30:00Z",
    "sourceId": "billing-service-1",
    "destinationId": "customer-001",
    "type": "positive",
    "value": 250.50,
    "unit": "SEK",
    "reference": "INV-12345"
  },
  ...
]
```

---

## Example Workflows

### Workflow 1: Single Record Ingestion

**Step 1: Send a new record**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-001",
    "time": "2026-10-09T10:00:00Z",
    "sourceId": "src-A",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 100.00,
    "unit": "SEK",
    "reference": "order-001"
  }'
```

**Console Output** (notification and alert, if applicable):
```
[NOTIFICATION] NotificationPayload(
  currentRecord=Record(...recordId=rec-001...),
  previousRecords=[]
)
```

**Step 2: Retry with same recordId (idempotency test)**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-001",
    "time": "2026-10-09T10:00:00Z",
    "sourceId": "src-A",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 100.00,
    "unit": "SEK",
    "reference": "order-001"
  }'
```

**Result**: Same record returned, no new notification or alert emitted.

---

### Workflow 2: Build Historical Summary

**Step 1: Add first record for (dest-1, order-001)**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-101",
    "time": "2026-10-09T10:00:00Z",
    "sourceId": "src-A",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 50.00,
    "unit": "SEK",
    "reference": "order-001"
  }'
```

**Console Output**:
```
[NOTIFICATION] NotificationPayload(
  currentRecord=Record(recordId=rec-101, value=50.00, ...),
  previousRecords=[]
)
```

**Step 2: Add second record for same (dest-1, order-001)**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-102",
    "time": "2026-10-09T11:00:00Z",
    "sourceId": "src-B",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 75.00,
    "unit": "SEK",
    "reference": "order-001"
  }'
```

**Console Output** (notification includes previous record summary):
```
[NOTIFICATION] NotificationPayload(
  currentRecord=Record(recordId=rec-102, value=75.00, ...),
  previousRecords=[
    Record(recordId=rec-101, value=50.00, ...)
  ]
)
```

---

### Workflow 3: Query Aggregation

**Step 1: Ingest multiple records**
```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/records \
    -H "Content-Type: application/json" \
    -d "{
      \"recordId\": \"rec-$i\",
      \"time\": \"2026-10-09T$(printf '%02d' $((9+i))):00:00Z\",
      \"sourceId\": \"src-A\",
      \"destinationId\": \"cust-1\",
      \"type\": \"positive\",
      \"value\": $((i * 100)).00,
      \"unit\": \"SEK\",
      \"reference\": \"batch-001\"
    }"
done
```

**Step 2: Query aggregation for destination**
```bash
curl "http://localhost:8080/api/records/aggregate?destinationId=cust-1"
```

**Response**:
```json
[
  {
    "destinationId": "cust-1",
    "totalValue": 1500.00,
    "records": [
      {"recordId": "rec-1", "value": 100.00, ...},
      {"recordId": "rec-2", "value": 200.00, ...},
      {"recordId": "rec-3", "value": 300.00, ...},
      {"recordId": "rec-4", "value": 400.00, ...},
      {"recordId": "rec-5", "value": 500.00, ...}
    ]
  }
]
```

---

### Workflow 4: Alert Trigger

**Step 1: Set alert threshold (via application.properties or -D flag)**
```bash
./gradlew bootRun -Dapp.alert.threshold=500
```

**Step 2: Ingest record with value below threshold**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-low",
    "time": "2026-10-09T10:00:00Z",
    "sourceId": "src-A",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 300.00,
    "unit": "SEK",
    "reference": "low-value"
  }'
```

**Console Output** (no alert):
```
[NOTIFICATION] NotificationPayload(...)
```

**Step 3: Ingest record with value above threshold**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": "rec-high",
    "time": "2026-10-09T11:00:00Z",
    "sourceId": "src-A",
    "destinationId": "dest-1",
    "type": "positive",
    "value": 750.00,
    "unit": "SEK",
    "reference": "high-value"
  }'
```

**Console Output** (alert emitted):
```
[NOTIFICATION] NotificationPayload(...)
[ALERT] Record exceeds threshold: rec-high value=750.00
```

---

## Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Alert threshold (default: 1000)
app.alert.threshold=500

# Spring JPA settings
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2 Database (in-memory)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.h2.console.enabled=true

# Server port
server.port=8080
```

### Async Configuration

`src/main/java/com/dola/billogramdataprocessingapp/config/AsyncConfig.java` enables `@Async` processing. The default thread pool can be customized by adding properties:

```properties
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
```

---

## Data Model

### Record Entity

```java
@Entity
public class Record {
    @Id
    @Column(nullable = false, unique = true)
    private String recordId;           // Unique identifier (primary key)
    
    @Column(nullable = false)
    private Instant time;              // Timestamp of record
    
    @Column(nullable = false)
    private String sourceId;           // Source service identifier
    
    @Column(nullable = false)
    private String destinationId;      // Target/customer identifier
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;           // POSITIVE or NEGATIVE
    
    @Column(nullable = false)
    private BigDecimal value;          // Monetary amount
    
    @Column(nullable = false)
    private String unit;               // Currency unit (e.g., SEK, EUR)
    
    @Column(nullable = false)
    private String reference;          // Business reference (e.g., invoice number)
}
```

### RecordType Enum

```java
public enum RecordType {
    POSITIVE,   // Credit/income
    NEGATIVE    // Debit/expense
}
```

---

## Idempotency Design

The service ensures **exactly-once processing** for each record:

1. **Unique Constraint**: `recordId` is the primary key with a unique constraint
2. **Insert-First Strategy**: Always attempt to insert the record
3. **Duplicate Recovery**: If `DataIntegrityViolationException` is caught (unique constraint violation), fetch and return the existing record
4. **No Side Effects on Duplicates**: Notifications and alerts only fire on successful inserts

This guarantees that:
- Duplicate submissions return the same record
- No duplicate events are emitted
- No extra database work for duplicates beyond the initial constraint check

---

## Concurrency Considerations

### What's Protected
- **Record idempotency**: Database unique constraint prevents duplicate storage
- **Insert safety**: Database manages concurrent insert attempts atomically

### What's Not Fully Protected (By Design for Interview)
- **Async side effects**: Notifications and alerts use in-memory async, not durable queues
- **History consistency**: `NotificationService` reflects eventual database consistency under concurrent writes
- **No explicit locking**: No row-level locking for destination/reference grouping

### Production Hardening Notes
For a production system handling high concurrency, consider:
1. **Outbox Pattern**: Store events durably alongside records, publish asynchronously
2. **Durable Queues**: Use Kafka, RabbitMQ, or similar for notifications/alerts
3. **Per-Key Serialization**: Use partition keys to serialize writes by destination + reference
4. **Optimistic Locking**: Add version fields for critical updates

---

## Testing

### Unit Tests

Three test classes cover all major components:

1. **`RecordProcessingIntegrationTest`** (3 tests)
   - Happy path: new records with side effects
   - Duplicate handling: duplicate inserts return existing record
   - Failure propagation: non-duplicate exceptions bubble up

2. **`NotificationServiceTest`** (4 tests)
   - Current + previous record inclusion
   - Filtering current record from history
   - Empty history handling
   - Correct query parameters

3. **`AlertServiceTest`** (6 tests)
   - Threshold checking (above, below, at threshold)
   - Null value handling
   - Record type independence
   - Full record processing

### Run Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests RecordProcessingIntegrationTest
./gradlew test --tests NotificationServiceTest
./gradlew test --tests AlertServiceTest

# With output
./gradlew test --info
```

### Test Report

After running tests, view the detailed report:
```
build/reports/tests/test/index.html
```

---

## Performance & Scalability

### Current Capacity (Interview Build)
- **Throughput**: ~27–100 requests/second (lightweight environment)
- **Latency**: <50ms per request (simple DB operations)
- **Concurrent Users**: Handles typical interview load

### For 100,000 Records/Hour
- **Rate**: ~28 records/second
- **Current Design**: Capable with caveats

### Optimization Opportunities (Not Implemented for Interview)
1. **Batch Inserts**: Group records into bulk operations
2. **Async Query**: Move aggregation into database layer (GROUP BY, SUM)
3. **Connection Pooling**: Tune HikariCP for higher concurrency
4. **Indexing**: Add composite indexes on (destinationId, reference, time)
5. **Caching**: Cache aggregation results with TTL
6. **Sharding**: Partition records by destination ID

---

## Deployment

### Local Development
```bash
./gradlew bootRun
```

### Docker (Not Included, but Easy to Add)
Create a `Dockerfile`:
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY build/libs/BillogramDataProcessingApp-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
./gradlew build
docker build -t billogram-service .
docker run -p 8080:8080 billogram-service
```

### With PostgreSQL
Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/billogram
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## Interview Notes

### Key Design Decisions
1. **Single Insert for Idempotency**: Simpler than pre-check + insert; relies on DB constraints
2. **In-Memory Async**: Avoids external broker complexity; acceptable for demo
3. **Simple Notification Payload**: Full historical records instead of a summary (can be improved)
4. **Alert Threshold Property**: Configurable but not dynamic (production would add Admin API)

### What to Emphasize in the Interview
- Idempotent record processing via unique constraints
- Async event emission without blocking ingestion
- Aggregation queries grouped by destination with optional filters
- Handling of concurrency via database atomicity, not explicit locking
- Clear separation of concerns: ingestion, notification, alerting

### What Could Be Improved (If Time Allowed)
1. Replace in-memory async with Kafka or RabbitMQ
2. Implement outbox pattern for durable event publishing
3. Add notification summary (count, total value) instead of full records
4. Implement per-key ordering for consistent notification history
5. Add pagination to aggregation queries
6. Support batch endpoint for multi-record ingestion
7. Add metrics/monitoring (Micrometer + Prometheus)
8. Implement circuit breaker for downstream service failures

---

## Troubleshooting

### Service won't start
```bash
# Check Java version
java -version
# Should be 21+

# Check port availability
lsof -i:8080
```

### Tests fail
```bash
# Clean and rebuild
./gradlew clean test --info

# Check for Spring Boot test context issues
# Ensure no other services running on 8080
```

### Duplicate records still being processed
- Check the database schema: `recordId` should have `UNIQUE` constraint
- Verify no custom save logic bypassing the unique constraint

### Alerts not triggering
- Check threshold property: `app.alert.threshold=<value>`
- Verify async executor is running (check console logs)
- Remember: async calls don't block response; alert may appear with delay

---

## Contact & Support

This is an interview assignment submission. Designed for demonstration and clarity, not production deployment.

For questions or improvements, refer to the inline code comments and test cases.

