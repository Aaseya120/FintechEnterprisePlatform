# Jira Epics, User Stories & Confluence Technical Specs

**Platform**: Fintech Enterprise Platform  
**Target Role**: Senior Java Developer / Microservices Developer (8+ Years Experience)  
**Tools**: Atlassian Jira Software, Atlassian Confluence

---

# Part 1: Jira Project Backlog

## Epic FIN-100: Microservices Architecture & Java Modernization
- **Lead**: Senior Java Architect
- **Goal**: Establish scalable, containerized Spring Boot 3.x microservices with Java 17/21 baseline while encapsulating legacy Java 8 accounting logic.

### User Stories:
#### `FIN-101`: Scaffold Multi-Module Maven Monorepo & Common Protobuf Contracts
- **Story Points**: 5
- **Priority**: High
- **Description**: As a Senior Engineer, I need a structured Maven monorepo with `common-contracts`, `api-gateway`, `order-service`, `payment-service`, and `legacy-core-accounting` so that teams can independently build and test services.
- **Acceptance Criteria**:
  - `mvn clean compile` succeeds across all 5 modules.
  - Protobuf generates Java classes (`PaymentProto.java`) via `os-maven-plugin`.
  - JaCoCo and SonarQube plugins configured in root POM.

#### `FIN-102`: Implement High-Throughput gRPC Payment Service
- **Story Points**: 8
- **Priority**: High
- **Description**: As a Payment Engineer, I need a gRPC service for `ProcessPayment`, `RefundPayment`, and `GetPaymentStatus` to ensure sub-10ms inter-service latency.
- **Acceptance Criteria**:
  - Binary gRPC server listening on port 9090.
  - Redis-backed idempotency filter to prevent duplicate payment execution.
  - Deadline timeouts configured on client stubs.

---

## Epic FIN-200: Distributed Transactions & Outbox Pattern
- **Lead**: Principal Backend Engineer
- **Goal**: Guarantee eventual consistency between Order creation, MinIO receipts, and asynchronous payment settlement without distributed database locking.

### User Stories:
#### `FIN-201`: Implement Transactional Outbox Pattern in Order Service
- **Story Points**: 8
- **Priority**: Blocker
- **Description**: As an Order Engineer, I need atomic database writes for both the Order entity and Outbox events within the same Oracle DB transaction to prevent message loss.
- **Acceptance Criteria**:
  - `FIN_OUTBOX_EVENTS` table populated inside `@Transactional` boundary.
  - Scheduled background worker (`OutboxPublisherJob`) polls pending records and dispatches to ActiveMQ.
  - Exponential backoff and retry counter up to 5 attempts.

#### `FIN-202`: Implement Saga Compensation Consumer for Failed Payments
- **Story Points**: 5
- **Priority**: High
- **Description**: As a System Architect, I need automated compensating transactions when payment risk checks fail so that customer orders are reverted to `CANCELLED`.
- **Acceptance Criteria**:
  - ActiveMQ listener on `fintech.saga.compensation.queue`.
  - Updates order status to `CANCELLED` and evicts or refreshes Redis cache.

---

## Epic FIN-300: Centralized Observability & Telemetry
- **Lead**: Senior DevOps / Platform Engineer
- **Goal**: Provide end-to-end W3C distributed tracing and Loki centralized log indexing.

### User Stories:
#### `FIN-301`: Integrate Micrometer Tracing & Grafana Loki Logback Appender
- **Story Points**: 5
- **Priority**: Medium
- **Description**: As an SRE, I need unified correlation between Zipkin trace IDs and Loki log lines across all microservices.
- **Acceptance Criteria**:
  - TraceId and SpanId injected automatically into Slf4j MDC.
  - Loki appender streams JSON logs to `http://localhost:3100/loki/api/v1/push`.

---

# Part 2: Confluence Architecture Decision Records (ADRs)

## ADR-001: Adoption of gRPC over REST for Inter-Service Payment Processing
- **Status**: APPROVED
- **Context**: Inter-service communication between `order-service` and `payment-service` handles millions of financial transactions daily. HTTP/1.1 JSON REST introduces excessive CPU serialization overhead and head-of-line blocking.
- **Decision**: Adopt gRPC over HTTP/2 with Protocol Buffers for synchronous synchronous inter-service communication.
- **Consequences**:
  - **Positive**: 7x reduction in payload size, multiplexing over single TCP connection, strict type-safe schemas.
  - **Negative**: Requires protobuf compiler plugins (`protobuf-maven-plugin`) and HTTP/2 ingress configuration.

## ADR-002: Adoption of Transactional Outbox Pattern instead of 2PC / XA
- **Status**: APPROVED
- **Context**: Coordinating distributed database transactions between Oracle and external messaging brokers using XA (Two-Phase Commit) degrades system throughput and leads to lock contention.
- **Decision**: Implement the Transactional Outbox Pattern with ActiveMQ and an idempotent consumer.
- **Consequences**:
  - **Positive**: High throughput, fault tolerance, no cross-service database locks.
  - **Negative**: Introduces eventual consistency instead of strict instant consistency.
