# 🎯 Interview Preparation — Quick-Fire Cheat Sheet
**Project:** Enterprise Core Banking Microservices Platform  
**Role:** Senior Java Developer (8+ Years)  
**Stack:** Java 21, Spring Boot 3.3, Spring Cloud, Kafka, PostgreSQL, Oracle 19c, Redis, Docker, K8s, AWS

---

## 1. MICROSERVICES ARCHITECTURE (13 Services)

| # | Service | Port | One-Line Purpose |
|---|---------|------|-----------------|
| 1 | `api-gateway` | 8080 | JWT auth, Redis rate limiting, CORS, route forwarding |
| 2 | `account-service` | 8081 | Ledger debit/credit, GraphQL, SOAP CBS bridge, term deposits, saving vaults |
| 3 | `payment-service` | 8082 | Saga transfers, outbox events, multi-rail gateways, standing instructions |
| 4 | `exchange-rate-service` | 8083 | Dynamic FX engine, IBAN/SWIFT validation, 60s locked quotes |
| 5 | `customer-service` | 8084 | Onboarding, KYC (AES-256-GCM), JWT auth, beneficiary cooling-off |
| 6 | `loan-service` | 8085 | Loan lifecycle, EMI amortization, foreclosure with penalty |
| 7 | `card-service` | 8086 | Luhn PAN, dynamic CVV, freeze/unfreeze, channel toggles, rewards |
| 8 | `fraud-detection-service` | 8087 | Redis velocity rules, geo-travel check, AI risk reasoning |
| 9 | `notification-service` | 8088 | Kafka consumers → omni-channel dispatch (Email/SMS/Push) |
| 10 | `reporting-service` | 8089 | Statement export (PDF/Excel/CSV/JSON), transaction import |
| 11 | `batch-service` | 8090 | Spring Batch, Oracle PL/SQL, clearing reconciliation |
| 12 | `bill-payment-service` | 8091 | Biller registry, telecom/utility gateway, settlement |
| 13 | `banking-common` | — | Shared: crypto, masking, idempotency, JWT, DTOs, exception handler |

---

## 2. DESIGN PATTERNS — Where & Why

| Pattern | Where Used | Interview Answer (1 line) |
|---------|-----------|--------------------------|
| **Saga Orchestrator** | `TransferSagaOrchestrator` | Coordinates debit→credit across services; auto-compensates if credit fails |
| **Transactional Outbox** | `OutboxPollerService` polls `outbox_events` table every 500ms | Prevents dual-write: DB commit + Kafka publish guaranteed atomic |
| **Strategy** | `PaymentGatewayManager`, `ExportStrategyFactory`, `BillerGatewayRegistry`, `NotificationManager` | Swap UPI/Card/NetBanking/PayPal processors without modifying calling code |
| **Factory** | `ReconciliationRuleFactory`, `ImportParserFactory` | Creates correct matching strategy or file parser based on config/extension |
| **Facade** | `ReconciliationFacade`, `TransactionReportingFacade` | Hides complex orchestration behind a single clean method call |
| **Decorator / AOP** | `IdempotencyAspect` (`@Idempotent`) | Intercepts duplicate requests using Redis TTL keys — no business code change |
| **Builder** | `CbsSoapEnvelopeBuilder` | Constructs complex SOAP XML envelopes step by step |

---

## 3. API ARCHITECTURE STYLES — All 6 Covered

| Style | Implementation | When to Use |
|-------|---------------|-------------|
| **REST** | All controllers (`@RestController`) | Standard CRUD, public-facing APIs |
| **GraphQL** | `AccountGraphQLController` (`@QueryMapping`, `@MutationMapping`) | Mobile dashboard — fetch balance + vaults + CBS in 1 call |
| **SOAP/XML** | `AccountSoapEndpoint`, `CbsSoapEnvelopeBuilder` | Legacy CBS/mainframe integration (Finacle/T24) |
| **Webhooks** | `PaymentWebhookController` with HMAC-SHA256 validation | Async callbacks from PayPal/Stripe/UPI |
| **WebSockets/SSE** | `ExchangeRateController` live ticker | Real-time FX rate push to mobile apps |
| **gRPC** | Inter-service Protobuf contracts | Sub-millisecond synchronous ledger checks |

---

## 4. SECURITY — Interview Points

- **JWT**: Custom `JwtTokenProvider` → HMAC-SHA256 → 15-min access + 7-day refresh token
- **Refresh Token Rotation (RTR)**: Old token destroyed on each refresh → prevents replay attacks
- **Spring Security**: Stateless filter chain → `JwtAuthenticationFilter` (OncePerRequestFilter) → `SecurityContextHolder`
- **RBAC**: `@PreAuthorize("hasRole('ROLE_ADMIN')")` — roles: CUSTOMER, TELLER, ADMIN, AUDITOR
- **AES-256-GCM**: Field-level encryption via JPA `AttributeConverter` → encrypts PII at rest (passport, SSN)
- **PCI-DSS Masking**: `DataMaskingUtil` → `4111-XXXX-XXXX-1234` in logs & API responses
- **Account Lockout**: 5 failed BCrypt attempts → `is_locked = true`
- **No PII in Kafka**: Credentials stripped from `banking.customer.onboarded` event payload

---

## 5. KAFKA — Event-Driven Architecture

- **Topics**: `banking.transactions.completed`, `banking.loan.disbursed`, `banking.card.blocked`, `banking.customer.onboarded`, `bill.payment.completed`
- **Outbox Pattern**: Write event to DB → poller reads every 500ms → publishes to Kafka → marks `PUBLISHED`
- **Dead Letter**: `MAX_RETRY_COUNT = 5` → moves to `DEAD_LETTER` status (not infinite loop)
- **KRaft Mode**: Kafka without ZooKeeper (single-process broker+controller)
- **Consumer Groups**: Each service has isolated consumer group (`notification-consumer-group`)

---

## 6. DATABASE — Quick Points

- **PostgreSQL 16**: Primary OLTP database for all services
- **Oracle 19c**: Legacy data warehouse, PL/SQL stored procedures for batch interest accrual & EOD reconciliation
- **Flyway**: Schema versioning with `baseline-on-migrate: true`
- **HikariCP**: Pool tuned (`max-pool: 20`, `min-idle: 5`, `max-lifetime: 1200000`)
- **JPA Indexing**: `@Index(name = "idx_transfers_saga", columnList = "saga_id, status")`
- **Monetary Columns**: `@Column(precision = 19, scale = 4)` — never `double`/`float`
- **Pessimistic Locking**: `@Lock(PESSIMISTIC_WRITE)` on account debit/credit to prevent race conditions
- **Outbox Table Index**: `idx_outbox_status_created (status, created_at)` — poller scans only PENDING rows

---

## 7. REDIS — Use Cases

| Use Case | How |
|----------|-----|
| **Caching** | `@Cacheable("exchangeRates")` with TTL, `@CacheEvict` on rate tick |
| **Rate Limiting** | Spring Cloud Gateway `RequestRateLimiter` with Redis token bucket |
| **Idempotency** | `Idempotency-Key` stored as Redis key with 24h TTL |
| **Fraud Velocity** | `ZADD` + `ZCOUNT` on sorted sets — count txns in last 60 seconds |
| **Session/Token** | Refresh token storage and rotation tracking |

---

## 8. RESILIENCE — Fault Tolerance

- **Circuit Breaker**: `@CircuitBreaker(name = "accountDownstream", fallbackMethod = "fallback")`
  - Sliding window: 10 calls, 50% failure rate → OPEN → wait 5s → HALF_OPEN → 3 probe calls
- **Retry**: `@Retry(name = "accountDatabase")` — max 3 attempts, exponential backoff (200ms × 2)
- **Fallback**: Returns cached/default response when downstream is down
- **Saga Compensation**: Scheduler detects transfers stuck `DEBITED` > 5 min → auto-refunds source account

---

## 9. DOCKER & KUBERNETES

- **Multi-stage Dockerfile**: `eclipse-temurin:21-jdk-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime)
- **Non-root user**: `adduser banking` → container runs as unprivileged user
- **JVM Tuning**: `MaxRAMPercentage=75%`, `G1GC`, `ExitOnOutOfMemoryError`, `HeapDumpOnOutOfMemoryError`
- **Docker Compose**: 12 app containers + PostgreSQL + Redis + Kafka KRaft + Prometheus + Grafana
- **Helm v3**: Rolling upgrades to AWS EKS with `--wait --timeout 5m`
- **HPA**: Horizontal Pod Autoscaler 3→30 pods based on CPU/memory

---

## 10. CI/CD PIPELINE (GitHub Actions — 5 Stages)

```
Checkout → JDK 21 Setup → mvn verify (JaCoCo) → SonarQube Scan → OWASP Dep Check → Docker Build → ECR Push → Helm Deploy to EKS
```

| Stage | Tool | What It Does |
|-------|------|-------------|
| Build & Test | Maven + JUnit 5 + Mockito | Compile, unit tests, JaCoCo coverage XML |
| Quality Gate | SonarQube | Code smells, vulnerabilities, coverage threshold |
| Security | OWASP dependency-check-maven | Fail build on CVSS ≥ 7 |
| Container | Multi-stage Docker | Minimal Alpine image → push to AWS ECR |
| Deploy | Helm v3 + EKS | Zero-downtime rolling upgrade in `production` namespace |

---

## 11. MONITORING & OBSERVABILITY

- **Spring Boot Actuator**: `/actuator/health` (K8s liveness/readiness), `/actuator/prometheus`
- **Micrometer + Prometheus**: JVM metrics, HTTP request latency, Kafka consumer lag
- **Grafana Dashboards**: Real-time service health visualization
- **OpenTelemetry**: Distributed tracing (`traceId`, `spanId` in every log line)
- **Correlation ID**: `X-Correlation-ID` header propagated across all microservices
- **Structured Logging**: `%d [%thread] [%X{traceId},%X{spanId}] %-5level %logger - %msg`

---

## 12. JAVA 21 FEATURES USED

| Feature | Where Used |
|---------|-----------|
| **Virtual Threads** | `spring.threads.virtual.enabled: true` in all services |
| **Records** | All DTOs: `TransferRequestDto`, `FraudCheckResultDto`, `LoanResponseDto` |
| **Pattern Matching Switch** | `TermDeposit.calculateMaturity()` — `case MONTHLY -> 12` |
| **Sealed Interfaces** | Service contracts and strategy interfaces |
| **Text Blocks** | SQL queries and SOAP envelope templates |
| **`var` type inference** | Local variables throughout codebase |

---

## 13. PAYMENT SERVICE — Deep Dive Interview

### Transfer State Machine:
```
INITIATED → DEBITED → COMPLETED (happy path)
INITIATED → DEBITED → COMPENSATING → COMPENSATED/FAILED (credit fails)
```

### Saga Flow (2-Phase):
1. Save `Transfer(INITIATED)` → call `accountClient.debit(source)` → update `DEBITED`
2. Call `accountClient.credit(target)` → success → `COMPLETED` → write outbox event
3. Credit fails → `COMPENSATING` → `accountClient.credit(source)` (refund) → `COMPENSATED`

### Payment Gateway Strategy Pattern:
- `PaymentGatewayManager` holds `List<PaymentProcessorStrategy>`
- Calls `.supports(channel)` → dispatches to `UpiPaymentProcessor`, `CardPaymentProcessor`, etc.
- UPI generates 12-digit NPCI RRN; Card validates 3DS; NetBanking routes NEFT/RTGS/IMPS

### Standing Instructions:
- Cron `@Scheduled(cron = "0 0 6 * * *")` → processes ACTIVE instructions due today
- Deterministic idempotency key: `si_exec_{id}_{date}` prevents duplicate charges

---

## 14. ACCOUNT SERVICE — Deep Dive Interview

- **Ledger Ops**: `debit()` and `credit()` with `@Transactional` + pessimistic lock
- **GraphQL**: `@QueryMapping accountByNumber`, `@MutationMapping debitAccount(accountNumber, amount)`
- **SOAP Bridge**: `LegacyCbsMiddlewareGateway` → `CbsSoapEnvelopeBuilder` → SOAP XML → circuit breaker
- **Term Deposit**: Pure BigDecimal compound interest: `A = P × (1 + r/n)^(n×t)` iterative calculation
- **Saving Vault**: Sub-account with auto round-up on linked account debits

---

## 15. FRAUD DETECTION — Deep Dive Interview

- **Velocity Rule**: Redis `ZADD(key, timestamp, txnId)` + `ZCOUNT(key, now-60s, now)` → if > 5 txns → HIGH_RISK
- **Amount Rule**: > $10,000 → 30 points; > $50,000 → 50 points
- **Geo Rule**: Source IP country ≠ account country → 25 points (impossible travel)
- **Risk Tiers**: 0-24 LOW → 25-49 MODERATE → 50-79 ELEVATED → 80+ CRITICAL
- **AI Advisor**: `AiFraudAdvisorService` → generates AML narrative + recommended action (HOLD/STEP_UP/MONITOR/APPROVE)

---

## 16. LOAN SERVICE — Deep Dive Interview

- **EMI Formula**: `EMI = P × r × (1+r)^n / ((1+r)^n - 1)` — guard for 0% interest (EMI = P/months)
- **Amortization Schedule**: Month-by-month breakdown of principal + interest components
- **Disbursement**: Publishes `banking.loan.disbursed` to Kafka → notification service alerts customer
- **Foreclosure**: Outstanding principal + accrued interest + 2% penalty → marks all installments PAID → loan CLOSED

---

## 17. CARD SERVICE — Deep Dive Interview

- **Luhn Algorithm**: Generates valid 16-digit PAN → double-every-other-digit → mod 10 check digit
- **Dynamic CVV**: HMAC-SHA256 of `cardId:cardNumber:timeWindow` → 3-digit → rotates every 5 min
- **PIN**: SHA-256 hashed, never stored in plain text
- **Channel Controls**: 5 independent toggles (online, ATM, POS, contactless, international)
- **Credit Eligibility**: Score ≥ 650 + income ≥ $25K → approved limit = income × 10-20%

---

## 18. BATCH & RECONCILIATION — Deep Dive Interview

- **Spring Batch 5**: Chunk-oriented (500k+ records), `ItemReader` → `ItemProcessor` → `ItemWriter`
- **Oracle PL/SQL**: `PKG_BANKING_CORE.SP_ACCRUE_DAILY_SAVINGS_INTEREST`, `PKG_RECONCILIATION_ENGINE.SP_RUN_AUTOMATED_RECON`
- **Reconciliation**: Facade + Strategy → Exact Match / Tolerance Window → detects breaks → stores audit trail
- **Break Resolution**: `resolveBreak(breakId, resolvedBy, notes)` with full audit tracking

---

## 19. NOTIFICATION SERVICE — Deep Dive Interview

- **Omni-Channel**: `NotificationManager.broadcastOmniChannel()` → iterates all `NotificationDispatcher` implementations
- **Dispatchers**: `EmailDispatcher`, `SmsDispatcher`, `PushDispatcher` — Strategy pattern
- **Kafka Topics**: Listens on `banking.transactions.completed`, `banking.loan.disbursed`, `banking.card.blocked`
- **Audit Log**: Every notification persisted in `notification_log` table with channel, ref, timestamp

---

## 20. EXCHANGE RATE SERVICE — Deep Dive Interview

- **Dynamic FX Engine**: `@Scheduled(fixedRate = 30000)` → Brownian motion rate simulation
- **Bid/Ask Spread**: Bank buys from customer at Bid (lower); sells at Ask (higher)
- **Pegged Currencies**: AED, SAR, HKD locked to USD (no fluctuation)
- **Locked Quote**: 60-second guaranteed FX rate with `quoteId` for cross-border settlement
- **IBAN Validation**: Country-specific length (DE=22, GB=22, IN=none) + SWIFT BIC format check
- **Redis Cache**: Rate cache invalidated on every market tick

---

## 21. PRODUCTION SUPPORT — RCA Scenarios

| Incident | Symptom | Root Cause | Fix |
|----------|---------|-----------|-----|
| **OOM Crash** | Pod exit code 137, GC > 2s | Unbounded `ConcurrentHashMap` cache without TTL | Replaced with Redis `@Cacheable` + TTL |
| **Thread Exhaustion** | 504 timeouts, 100% tomcat threads | Virtual Thread pinned by `synchronized` + JDBC I/O | Replaced with `ReentrantLock` |
| **Slow API** | p99 latency 12s (was 15ms) | HikariCP pool exhausted, queries holding connections | Reduced `max-lifetime`, added connection leak detection |
| **Stalled Transfers** | DEBITED status stuck > 5 min | Network drop between debit and credit calls | Auto-compensation scheduler + saga rollback |

---

## 22. DATABASE TUNING — Key Indexes

```sql
-- Customer account lookup (high selectivity)
CREATE INDEX idx_accounts_customer_status ON accounts (customer_id, status);

-- Outbox poller (skip millions of PUBLISHED rows)
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);

-- Transfer saga query
CREATE INDEX idx_transfers_saga ON transfers (saga_id, status);

-- Transfer history by account
CREATE INDEX idx_transfers_src_created ON transfers (source_account, created_at);
```

---

## 23. COMMON INTERVIEW Q&A — Quick Answers

| Question | Answer |
|----------|--------|
| How do you handle distributed transactions? | Saga Orchestrator pattern with compensating rollbacks, not 2PC |
| How do you prevent duplicate payments? | Redis idempotency keys with 24h TTL via `@Idempotent` AOP aspect |
| How do you ensure Kafka + DB consistency? | Transactional Outbox — write event to DB, poller relays to Kafka |
| How do you secure REST APIs? | Stateless JWT (HMAC-SHA256) + Spring Security filter chain + RBAC |
| How do you encrypt sensitive data at rest? | AES-256-GCM via JPA `AttributeConverter` — transparent encrypt/decrypt |
| How do you handle service failures? | Resilience4j CircuitBreaker + Retry + fallback; saga auto-compensation |
| Why GraphQL alongside REST? | Mobile apps need single-call dashboard composition (balance + vaults + CBS) |
| Why SOAP in a modern system? | Legacy CBS mainframe integration — banks don't rewrite core systems |
| How do you monitor in production? | Actuator + Prometheus + Grafana + Splunk + correlation IDs + traceId |
| How do you deploy to production? | GitHub Actions → SonarQube → OWASP → Docker → ECR → Helm → EKS |
| How do you prevent fraud? | Redis sorted-set velocity detection + geo-travel + amount thresholds + AI advisor |
| How do you handle 0% interest loans? | Guard clause: if rate == 0 → EMI = principal/months (prevents division by zero) |
| What Java 21 features do you use? | Virtual Threads, Records, Pattern Matching Switch, Sealed Interfaces |
| How do you handle stalled transfers? | Scheduler scans every 60s for transfers stuck in DEBITED > 5 min → auto-refund |

---

> 💡 **Tip:** When answering interview questions, always reference the **exact class name** and **pattern name** — e.g., *"We used the Saga Orchestrator pattern in `TransferSagaOrchestrator` to coordinate distributed debit-credit flows with automatic compensating rollbacks."*
