# Engineering Standards, Code Review & Developer Mentorship Guidelines
**Target Audience:** Core Banking Engineering Teams, Junior/Mid Developers, QA Engineers, Security Reviewers  
**Platform Standard:** Java 21, Spring Boot 3.3+, Spring Cloud, Kafka, PostgreSQL / Oracle 19c

---

## 1. Core Architectural Principles

### 1.1 Never Use Float or Double for Currency
- **Rule**: All financial calculations must strictly use `BigDecimal` with explicit scale and `RoundingMode.HALF_UP`.
- **Reason**: Binary floating-point representation (`double`, `float`) introduces catastrophic rounding errors in ledger accounting (e.g. `0.1 + 0.2 = 0.30000000000000004`).
- **Code Review Check**:
  ```java
  // REJECTED
  double balance = account.getBalance() - amount;

  // APPROVED
  BigDecimal balance = account.getBalance().subtract(amount).setScale(4, RoundingMode.HALF_UP);
  ```

### 1.2 Mandatory Financial Idempotency
- **Rule**: Every HTTP `POST`, `PUT`, or state-altering mutation must require the `Idempotency-Key` header.
- **Reason**: Mobile network disconnects and client retry loops can duplicate payment debits.
- **Implementation**: Decorate controller handlers with `@Idempotent(headerName = "Idempotency-Key", ttl = 24)`.

### 1.3 Outbox Pattern Over Dual Writes
- **Rule**: Never execute a database write followed immediately by a direct Kafka `send()` call inside the same method.
- **Reason**: If Kafka is down or a network blip occurs after the DB commit, the event is permanently lost, causing cross-microservice state divergence.
- **Approved Pattern**: Persist an `OutboxEventEntity` within the database transaction; allow the asynchronous, resilient `OutboxPollerService` to relay events to Kafka with at-least-once delivery guarantees.

---

## 2. Modern Java 21 & Spring Boot Standards

### 2.1 Use Java Records for DTOs and Kafka Events
```java
// Immutable, thread-safe, transparent data carriers
public record TransferRequestDto(
    @NotBlank String sourceAccountNumber,
    @NotBlank String targetAccountNumber,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String currency
) implements Serializable {}
```

### 2.2 Virtual Threads Concurrency Discipline
- Avoid long-running `synchronized` blocks inside I/O operations (causes Virtual Thread carrier thread pinning).
- Use `ReentrantLock` or functional concurrent primitives.

---

## 3. Testing Best Practices (JUnit 5, Mockito & Testcontainers)

### 3.1 Unit Testing Guidelines
1. **Naming**: Follow BDD convention: `should[ExpectedBehavior]When[StateUnderTest]()`.
2. **Mocking**: Mock only external boundaries (Repositories, Clients). Do not mock domain entities (`Account`, `Transfer`).
3. **Assertions**: Use AssertJ fluent assertions (`assertThat(result).isNotNull()`).
4. **Coverage Standard**: Enforce minimum 80% branch coverage on business logic via JaCoCo.

---

## 4. Secure Coding & OWASP Compliance Standards

| Security Domain | Mandatory Standard |
| :--- | :--- |
| **Authentication** | Bearer JWT signed via HMAC-SHA256 with refresh token rotation |
| **Authorization** | Strict RBAC (`@PreAuthorize("hasRole('ROLE_CUSTOMER')")`) |
| **SQL Injection** | Exclusively JPA parameter binding (`:param`), Hibernate Criteria, or Flyway migrations. Native string concatenation is blocked. |
| **Secret Management** | Zero plaintext credentials in Git. Injected via AWS Secrets Manager or Kubernetes Secrets. |
| **Audit Compliance** | Every balance mutation writes an immutable record to `account_audit_log` with actor ID, IP address, and correlation ID. |

---

## 5. Code Review Checklist for Pull Requests
Before approving any PR to `develop` or `main`:
- [ ] Financial calculations use `BigDecimal` with rounding modes.
- [ ] Concurrency risks evaluated (pessimistic lock on debit/credit, optimistic lock version).
- [ ] Unit tests written with JUnit 5 & Mockito covering happy path and all business exception paths.
- [ ] Swagger OpenAPI annotations added with descriptive error codes.
- [ ] Flyway migration SQL tested for backwards compatibility.
- [ ] No carrier-pinning `synchronized` blocks on I/O paths.
- [ ] Metrics (`Timer`, `Counter`) or Actuator health indicators included for new external integrations.
