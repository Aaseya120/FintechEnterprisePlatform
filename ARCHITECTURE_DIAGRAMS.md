# 🏗️ Architecture & Flow Diagrams — Interview Reference

---

## 🔥 End-to-End Request Lifecycle (All Layers)

*How a single API request flows from client to database and back — every layer touched.*

```mermaid
flowchart TD
    subgraph CLIENT["👤 Client Layer"]
        MOB["Mobile App<br/>(iOS / Android)"]
        WEB["Web App<br/>(React / Angular)"]
    end

    subgraph GATEWAY["🔒 API Gateway Layer (:8080)"]
        CORS["CORS Filter"]
        JWT_F["JWT Authentication<br/>Filter"]
        RL["Redis Rate Limiter<br/>(Token Bucket)"]
        ROUTER["Route Matcher<br/>(Path → Service)"]
    end

    subgraph SERVICE["⚙️ Microservice Layer (e.g. Payment :8082)"]
        CTRL["@RestController<br/>PaymentController"]
        VALID["Bean Validation<br/>(@Valid, @NotNull)"]
        IDEMP["Idempotency Check<br/>(Redis Key Lookup)"]
        SVC["@Service<br/>Business Logic"]
        SAGA["Saga Orchestrator<br/>(if distributed txn)"]
        CLIENT_HTTP["RestClient / WebClient<br/>(Inter-Service Call)"]
    end

    subgraph RESILIENCE["🛡️ Resilience Layer"]
        CB["Circuit Breaker<br/>(Resilience4j)"]
        RETRY["Retry with<br/>Exponential Backoff"]
        FB["Fallback<br/>Method"]
    end

    subgraph DATA["💾 Data Layer"]
        REPO["Spring Data JPA<br/>Repository"]
        CACHE["Redis Cache<br/>(@Cacheable)"]
        FLYWAY["Flyway<br/>Migrations"]
        PG[(PostgreSQL)]
        ORA[(Oracle 19c)]
    end

    subgraph EVENTS["📨 Event Layer"]
        OUTBOX["Outbox Table<br/>(outbox_events)"]
        POLLER["Outbox Poller<br/>(Scheduled)"]
        KAFKA["Apache Kafka<br/>(KRaft)"]
        CONSUMER["Kafka Consumer<br/>(Notification / Fraud)"]
    end

    subgraph SECURITY["🔐 Security Layer (Cross-Cutting)"]
        ENC["AES-256-GCM<br/>Field Encryption"]
        MASK["PCI-DSS<br/>Data Masking"]
        AUDIT["Audit Logging<br/>(Correlation ID)"]
    end

    subgraph MONITOR["📊 Observability Layer"]
        ACT["Actuator<br/>Health / Metrics"]
        PROM["Prometheus<br/>Scrape"]
        GRAF["Grafana<br/>Dashboards"]
        TRACE["Distributed Tracing<br/>(traceId / spanId)"]
    end

    MOB -->|HTTPS / TLS| CORS
    WEB -->|HTTPS / TLS| CORS
    CORS --> JWT_F
    JWT_F --> RL
    RL --> ROUTER
    ROUTER -->|Forward| CTRL

    CTRL --> VALID
    VALID --> IDEMP
    IDEMP --> SVC
    SVC --> SAGA
    SAGA --> CLIENT_HTTP

    CLIENT_HTTP --> CB
    CB --> RETRY
    RETRY --> FB

    SVC --> REPO
    SVC --> CACHE
    REPO --> PG
    REPO --> ORA

    SVC --> OUTBOX
    OUTBOX --> POLLER
    POLLER --> KAFKA
    KAFKA --> CONSUMER

    SVC --> ENC
    SVC --> MASK
    SVC --> AUDIT

    ACT --> PROM
    PROM --> GRAF
    SVC --> TRACE

    style GATEWAY fill:#e8f4fd,stroke:#2196F3
    style SERVICE fill:#e8f5e9,stroke:#4CAF50
    style DATA fill:#fff3e0,stroke:#FF9800
    style EVENTS fill:#f3e5f5,stroke:#9C27B0
    style SECURITY fill:#fce4ec,stroke:#E91E63
    style MONITOR fill:#e0f2f1,stroke:#009688
    style RESILIENCE fill:#fff8e1,stroke:#FFC107
```

---

## 🏛️ Layered Architecture Stack

```mermaid
block-beta
    columns 1
    block:PRESENTATION["🖥️ PRESENTATION LAYER"]
        A["Mobile (iOS/Android)"] B["Web (React/Angular)"] C["Third-Party (Webhooks)"]
    end
    block:GATEWAY_L["🔒 API GATEWAY LAYER"]
        D["Spring Cloud Gateway → JWT Auth → Rate Limit → CORS → Route"]
    end
    block:API_L["📡 API LAYER (Controllers)"]
        E["REST Controllers"] F["GraphQL Controllers"] G["SOAP Endpoints"] H["Webhook Receivers"]
    end
    block:BUSINESS_L["⚙️ BUSINESS LOGIC LAYER (Services)"]
        I["Account Service"] J["Payment + Saga"] K["Loan Service"] L["Card Service"] M["Fraud Engine"]
    end
    block:INTEGRATION_L["🔗 INTEGRATION LAYER"]
        N["Kafka Producer"] O["Redis Cache"] P["RestClient (Inter-Svc)"] Q["SOAP CBS Client"]
    end
    block:DATA_L["💾 DATA ACCESS LAYER"]
        R["Spring Data JPA + Hibernate + Flyway"]
    end
    block:INFRA_L["🏗️ INFRASTRUCTURE LAYER"]
        S["PostgreSQL"] T["Oracle 19c"] U["Redis"] V["Kafka (KRaft)"] W["Prometheus + Grafana"]
    end

    PRESENTATION --> GATEWAY_L
    GATEWAY_L --> API_L
    API_L --> BUSINESS_L
    BUSINESS_L --> INTEGRATION_L
    INTEGRATION_L --> DATA_L
    DATA_L --> INFRA_L
```

---

## 🔄 Complete Fund Transfer — End-to-End Through All Layers

*The most important flow in the entire platform — trace every layer a payment touches.*

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 👤 Customer (Mobile)
    participant GW as 🔒 API Gateway<br/>:8080
    participant JWT as JWT Filter
    participant RL as Rate Limiter<br/>(Redis)
    participant PC as PaymentController<br/>:8082
    participant IDEMP as Idempotency<br/>(Redis)
    participant PS as PaymentService
    participant FRAUD as FraudService<br/>:8087
    participant SAGA as SagaOrchestrator
    participant ACC as AccountService<br/>:8081
    participant CB as CircuitBreaker
    participant DB as PostgreSQL
    participant OUTBOX as Outbox Table
    participant POLLER as Outbox Poller
    participant KAFKA as Apache Kafka
    participant NOTIF as NotificationService<br/>:8088

    Note over Customer,GW: ── GATEWAY LAYER ──
    Customer->>GW: POST /api/v1/payments/transfers<br/>+ Bearer JWT + Idempotency-Key
    GW->>JWT: Validate HMAC-SHA256 token
    JWT->>JWT: Verify signature, expiry, extract roles
    JWT->>RL: Check Redis rate limit bucket
    RL-->>GW: ✅ Within limit
    GW->>PC: Route to Payment Service

    Note over PC,IDEMP: ── CONTROLLER LAYER ──
    PC->>PC: @Valid — validate request body
    PC->>IDEMP: Check Idempotency-Key in Redis
    IDEMP-->>PC: ✅ First request (not duplicate)

    Note over PS,FRAUD: ── BUSINESS LAYER (Fraud Check) ──
    PC->>PS: initiateTransfer()
    PS->>FRAUD: checkFraudRisk(txn)
    FRAUD->>FRAUD: Redis ZADD velocity + geo check
    FRAUD-->>PS: APPROVE (score: 12)

    Note over SAGA,ACC: ── BUSINESS LAYER (Saga Execution) ──
    PS->>DB: Save Transfer (status: INITIATED)
    PS->>SAGA: executeSaga()

    SAGA->>CB: Debit source via CircuitBreaker
    CB->>ACC: POST /accounts/{src}/debit
    ACC->>DB: UPDATE balance (pessimistic lock)
    ACC-->>SAGA: ✅ Debited
    SAGA->>DB: Update Transfer (DEBITED)

    SAGA->>CB: Credit target via CircuitBreaker
    CB->>ACC: POST /accounts/{tgt}/credit
    ACC->>DB: UPDATE balance
    ACC-->>SAGA: ✅ Credited
    SAGA->>DB: Update Transfer (COMPLETED)

    Note over OUTBOX,NOTIF: ── EVENT LAYER (Async) ──
    PS->>OUTBOX: INSERT outbox_events (PENDING)
    POLLER->>OUTBOX: Poll every 500ms
    POLLER->>KAFKA: Publish transfer.completed
    POLLER->>OUTBOX: UPDATE status → PUBLISHED
    KAFKA->>NOTIF: Consume event
    NOTIF->>NOTIF: Send SMS + Email + Push

    Note over PS,Customer: ── RESPONSE ──
    PS-->>PC: TransferResponse
    PC-->>GW: 200 OK
    GW-->>Customer: Transfer COMPLETED ✅
```

---

## 1. High-Level System Architecture

```mermaid
graph TD
    Client["Digital Channels<br/>(iOS / Android / Web)"] -->|HTTPS| GW["API Gateway :8080<br/>JWT Auth • Redis Rate Limiting"]

    GW --> ACC["Account Service :8081<br/>Ledger • GraphQL • SOAP CBS"]
    GW --> PAY["Payment Service :8082<br/>Transfers • Saga • Outbox"]
    GW --> FX["Forex Service :8083<br/>FX Rates • Quotes • IBAN"]
    GW --> CUST["Customer Service :8084<br/>Onboarding • KYC • Auth"]
    GW --> LOAN["Loan Service :8085<br/>EMI • Amortization"]
    GW --> CARD["Card Service :8086<br/>PAN • CVV • Controls"]
    GW --> FRAUD["Fraud Service :8087<br/>Velocity • Geo • AI"]
    GW --> REP["Reporting :8089<br/>PDF • Excel • CSV"]
    GW --> BATCH["Batch Service :8090<br/>Reconciliation • PL/SQL"]
    GW --> BILL["Bill Payment :8091<br/>Biller Gateway"]

    PAY -->|Outbox Poller| KAFKA["Apache Kafka"]
    BILL -->|Events| KAFKA
    LOAN -->|Events| KAFKA
    CARD -->|Events| KAFKA
    KAFKA --> NOTIF["Notification :8088<br/>Email • SMS • Push"]

    ACC --> DB[(PostgreSQL)]
    PAY --> DB
    LOAN --> DB
    CARD --> DB
    ACC --> REDIS[(Redis)]
    FX --> REDIS
    FRAUD --> REDIS
    GW --> REDIS

    ACC -->|SOAP XML| CBS["Legacy CBS<br/>Mainframe"]
```

---

## 2. Fund Transfer — Saga Orchestrator Flow

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant GW as API Gateway
    participant PAY as Payment Service
    participant FRAUD as Fraud Detection
    participant ACC as Account Service
    participant DB as Database
    participant KAFKA as Kafka
    participant NOTIF as Notification

    Customer->>GW: POST /api/v1/payments/transfers
    GW->>PAY: Route to Payment Service

    rect rgb(240, 245, 255)
    Note over PAY,FRAUD: Step 1: Fraud Check
    PAY->>FRAUD: Check velocity & geo rules
    FRAUD-->>PAY: APPROVE (risk score: 12)
    end

    rect rgb(245, 255, 245)
    Note over PAY,ACC: Step 2: Debit Source Account
    PAY->>DB: Save Transfer (INITIATED)
    PAY->>ACC: Debit source account
    ACC-->>PAY: Debit success
    PAY->>DB: Update Transfer (DEBITED)
    end

    rect rgb(255, 250, 240)
    Note over PAY,ACC: Step 3: Credit Target Account
    PAY->>ACC: Credit target account
    alt Credit Succeeds
        ACC-->>PAY: Credit success
        PAY->>DB: Update Transfer (COMPLETED)
        PAY->>DB: Write outbox event
    else Credit Fails
        Note over PAY,ACC: COMPENSATION: Auto-refund
        PAY->>ACC: Refund source account
        PAY->>DB: Transfer (COMPENSATED / FAILED)
    end
    end

    rect rgb(250, 245, 255)
    Note over PAY,NOTIF: Step 4: Async Notification
    PAY->>KAFKA: Publish transfer event
    KAFKA->>NOTIF: Consume → Send SMS/Push
    end

    PAY-->>Customer: Transfer result
```

---

## 3. Transfer State Machine

```mermaid
stateDiagram-v2
    [*] --> INITIATED
    INITIATED --> DEBITED : Source account debited
    DEBITED --> COMPLETED : Target credited successfully
    DEBITED --> COMPENSATING : Target credit failed
    COMPENSATING --> COMPENSATED : Source refunded
    COMPENSATED --> [*]
    COMPLETED --> [*]

    note right of DEBITED : Recovery scheduler scans<br/>transfers stuck here > 5 min
    note right of COMPENSATING : Auto-refund triggered
```

---

## 4. JWT Authentication & Token Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor User as Mobile / Web
    participant Auth as Auth Controller
    participant Filter as JWT Filter
    participant API as Banking APIs
    participant DB as Customer DB

    Note over User, DB: Login
    User->>Auth: POST /auth/login (username, password)
    Auth->>DB: Verify BCrypt password
    Auth-->>User: Access Token (15 min) + Refresh Token (7 day)

    Note over User, API: Authenticated Request
    User->>Filter: GET /accounts (Bearer token)
    Filter->>Filter: Verify HMAC-SHA256 signature & expiry
    Filter->>API: Set SecurityContext → execute request
    API-->>User: 200 OK (account data)

    Note over User, Auth: Token Refresh
    User->>Filter: GET /accounts (expired token)
    Filter-->>User: 401 Unauthorized
    User->>Auth: POST /auth/refresh (refresh token)
    Auth->>DB: Validate & rotate refresh token
    Auth-->>User: New access + refresh tokens
```

---

## 5. Event-Driven Architecture (Kafka + Outbox)

```mermaid
flowchart LR
    subgraph Payment Service
        API[REST Controller] --> BL[Business Logic]
        BL --> DB[(Database)]
        BL --> OT[Outbox Table]
        OP[Outbox Poller<br/>every 500ms] --> OT
    end

    OP -->|Publish| KAFKA[Apache Kafka]

    subgraph Consumers
        KAFKA --> NS[Notification Service<br/>SMS / Email / Push]
        KAFKA --> FS[Fraud Service<br/>Velocity Update]
        KAFKA --> RS[Reporting Service<br/>Audit Log]
    end

    style OT fill:#fff3cd
    style KAFKA fill:#e2e3e5
```

---

## 6. API Gateway — Rate Limiting & Routing

```mermaid
flowchart TD
    CLIENT[Client Request] --> JWT{JWT Valid?}
    JWT -->|No| REJECT[401 Unauthorized]
    JWT -->|Yes| RL{Rate Limit<br/>Exceeded?}
    RL -->|Yes| THROTTLE[429 Too Many Requests]
    RL -->|No| ROUTE{Route Matching}

    ROUTE --> ACC["/accounts → :8081"]
    ROUTE --> PAY["/payments → :8082"]
    ROUTE --> FX["/exchange-rates → :8083"]
    ROUTE --> CUST["/customers → :8084"]
    ROUTE --> LOAN["/loans → :8085"]
    ROUTE --> CARD["/cards → :8086"]
    ROUTE --> FRAUD["/fraud → :8087"]
    ROUTE --> BILL["/bills → :8091"]
```

---

## 7. Fraud Detection — Real-Time Risk Scoring

```mermaid
flowchart TD
    TXN[Incoming Transaction] --> VEL{Velocity Check<br/>Redis ZADD/ZCOUNT}
    VEL -->|"> 5 txns in 60s"| HIGH[+40 Risk Points]
    VEL -->|Normal| AMT{Amount Check}

    AMT -->|"> $10K"| MED[+30 Risk Points]
    AMT -->|Normal| GEO{Geo Check<br/>Source ≠ Account Country}

    GEO -->|Mismatch| GEO_SCORE[+25 Risk Points]
    GEO -->|Match| PASS[+0 Points]

    HIGH --> TOTAL[Total Risk Score]
    MED --> TOTAL
    GEO_SCORE --> TOTAL
    PASS --> TOTAL

    TOTAL --> TIER{Risk Tier}
    TIER -->|"0-24"| APPROVE[AUTO APPROVE]
    TIER -->|"25-49"| MONITOR[POST-SETTLEMENT MONITOR]
    TIER -->|"50-79"| STEP_UP[STEP-UP AUTH]
    TIER -->|"80-100"| BLOCK[IMMEDIATE HOLD]
```

---

## 8. CI/CD Pipeline

```mermaid
flowchart LR
    GIT[Git Push] --> BUILD[Maven Build<br/>+ JUnit Tests]
    BUILD --> COV[JaCoCo<br/>Coverage]
    COV --> SONAR[SonarQube<br/>Quality Gate]
    SONAR --> OWASP[OWASP<br/>Dep Check]
    OWASP --> DOCKER[Docker<br/>Multi-Stage Build]
    DOCKER --> ECR[Push to<br/>AWS ECR]
    ECR --> HELM[Helm Deploy<br/>to AWS EKS]

    style SONAR fill:#fff3cd
    style HELM fill:#d4edda
```

---

## 9. Reconciliation Engine Flow

```mermaid
flowchart TD
    TRIGGER[Trigger Recon Run] --> FETCH_EXT[Fetch External<br/>Clearing Records]
    FETCH_EXT --> FETCH_INT[Load Internal<br/>Ledger Records]
    FETCH_INT --> STRATEGY{Matching Strategy}

    STRATEGY -->|Exact Match| EM[Match by<br/>Reference ID + Amount]
    STRATEGY -->|Tolerance Window| TW[Match within<br/>±threshold amount]

    EM --> RESULT[Matched / Unmatched]
    TW --> RESULT

    RESULT --> MATCHED[Matched Count<br/>+ Match Rate %]
    RESULT --> BREAKS[Reconciliation Breaks<br/>Amount Mismatch / Orphans]

    BREAKS --> PERSIST[Persist Breaks<br/>with Audit Trail]
    PERSIST --> RESOLVE[Manual Resolution<br/>by Auditor]
```

---

## 10. Microservices Communication Map

```mermaid
flowchart TD
    GW[API Gateway] -->|Sync REST| ACC[Account]
    GW -->|Sync REST| PAY[Payment]
    GW -->|Sync REST| CUST[Customer]
    GW -->|Sync REST| LOAN[Loan]
    GW -->|Sync REST| CARD[Card]
    GW -->|Sync REST| FX[Forex]
    GW -->|Sync REST| BILL[Bill Pay]
    GW -->|GraphQL| ACC

    PAY -->|Sync REST| ACC
    PAY -->|Sync REST| FRAUD[Fraud]
    PAY -->|Async Kafka| NOTIF[Notification]
    LOAN -->|Async Kafka| NOTIF
    CARD -->|Async Kafka| NOTIF
    BILL -->|Async Kafka| NOTIF
    CUST -->|Async Kafka| NOTIF

    ACC -->|SOAP/XML| CBS[Legacy CBS]

    ACC -->|R/W| DB[(PostgreSQL)]
    PAY -->|R/W| DB
    LOAN -->|R/W| DB
    CARD -->|R/W| DB
    BATCH[Batch] -->|PL/SQL| ORACLE[(Oracle 19c)]

    FX -->|Cache| REDIS[(Redis)]
    FRAUD -->|Sorted Sets| REDIS
    GW -->|Rate Limit| REDIS
```

---

> 💡 **Interview Tip:** Open this file on your laptop before the interview. If the interviewer asks *"Can you draw the architecture?"* — you already have it ready. Walk through diagram #1 for the big picture, then zoom into #2 (Saga) or #5 (Outbox) based on what they ask.
