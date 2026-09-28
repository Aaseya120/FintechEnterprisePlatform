# Enterprise Microservices Architecture Specification

This document details the high-level architecture, distributed transaction model, distributed tracing, and data storage design for the **Fintech Enterprise Platform**, tailored to the technical standard of a **Senior Java Developer / Microservices Developer with 8+ years of experience**.

---

## 1. System Architecture Overview

```mermaid
flowchart TB
    subgraph Clients["Clients & External Consumers"]
        WEB["Single Page App / Mobile"]
        PARTNER["External FinTech API Clients"]
        POSTMAN["Postman / Automated Test Suite"]
    end

    subgraph BDNS_Layer["Enterprise DNS & Routing Layer"]
        BDNS["BDNS (Banking Domain Name System)\napi.fintech.enterprise.internal"]
    end

    subgraph Gateway_Cluster["Edge & Security Layer"]
        GW["Spring Cloud Gateway (Port 8080)\n- Netty Reactive Engine\n- Spring Security OAuth2 / JWT\n- Redis Distributed Rate Limiter\n- Distributed Trace Injector"]
    end

    subgraph Microservices["Core Microservices Domain"]
        subgraph Order_Domain["Order Service (Port 8081)"]
            OS["Order Service (Spring Boot 3.3.x)\n- Spring Data JPA\n- Transactional Outbox\n- MinIO S3 Receipt Generator"]
        end

        subgraph Payment_Domain["Payment Service (Port 8082, gRPC: 9090)"]
            PS["Payment Service (Spring Boot 3.3.x)\n- High-Speed gRPC Server\n- ActiveMQ Saga Listener\n- Compensating Transaction Engine"]
        end

        subgraph Legacy_Domain["Legacy Core Banking (Java 8 / WebLogic Baseline)"]
            LA["Legacy Accounting Engine & EJB/SOA\n- Remote EJB (AccountPostingEJB)\n- JAX-WS SOA Service (CoreBankingPostingService)\n- Immutable Double-Entry Ledger"]
        end
    end

    subgraph Messaging_and_Cache["Event Streaming & In-Memory State"]
        MQ["ActiveMQ Artemis (Port 61616 / 8161)\n- fintech.order.events.queue\n- fintech.saga.compensation.queue"]
        REDIS["Redis In-Memory Cache (Port 6379)\n- Order Details Caching\n- Idempotency Deduplication\n- Gateway Rate Limiting Buckets"]
    end

    subgraph Storage_Layer["Databases & Object Storage"]
        ORACLE["Oracle Database (Port 1521)\n- FIN_ORDERS\n- FIN_OUTBOX_EVENTS"]
        MINIO["MinIO S3 Compatible Storage (Port 9000 / 9001)\n- Bucket: fintech-invoices\n- Tamper-proof Financial Receipts"]
    end

    subgraph Observability_Stack["Centralized Observability & Telemetry"]
        ZIPKIN["Zipkin Tracing (Port 9411)\nW3C Trace Context (traceparent)"]
        LOKI["Grafana Loki (Port 3100)\nStructured JSON Log Aggregation"]
        GRAFANA["Grafana Dashboards (Port 3000)"]
    end

    %% Client flows
    WEB --> BDNS
    PARTNER --> BDNS
    POSTMAN --> BDNS
    BDNS --> GW

    %% Gateway Routing
    GW -->|"REST / JWT"| OS
    GW -->|"REST / Admin"| PS

    %% Inter-service synchronous & asynchronous
    OS -->|"gRPC HTTP/2 (Port 9090)"| PS
    OS -->|"Publish Outbox Events"| MQ
    MQ -->|"Consume Order Events"| PS
    PS -->|"Trigger Compensation"| MQ
    MQ -->|"Consume Compensation"| OS
    PS -->|"SOA SOAP / EJB Bridge"| LA

    %% Persistence flows
    OS -->|"JPA / JDBC"| ORACLE
    OS -->|"Cache Order & Idem"| REDIS
    OS -->|"Upload Invoices"| MINIO
    PS -->|"Deduplication"| REDIS

    %% Telemetry flows
    GW -.->|"Trace & Spans"| ZIPKIN
    OS -.->|"Trace & Spans"| ZIPKIN
    PS -.->|"Trace & Spans"| ZIPKIN
    GW -.->|"Push Logs"| LOKI
    OS -.->|"Push Logs"| LOKI
    PS -.->|"Push Logs"| LOKI
    LOKI --> GRAFANA
```

---

## 2. Distributed Transactions: Transactional Outbox & Saga Pattern

In distributed financial architectures, **Two-Phase Commit (2PC)** introduces synchronous blocking and single points of failure that do not scale in cloud-native environments. This platform implements the **Transactional Outbox Pattern** combined with a **Choreographed / Orchestrated Saga**:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway
    participant OS as Order Service
    participant DB as Oracle DB (Orders & Outbox)
    participant S3 as MinIO S3
    participant PS as Payment Service (gRPC)
    participant MQ as ActiveMQ (JMS)

    Client->>GW: POST /api/orders (JWT + Idempotency-Key)
    GW->>GW: Validate JWT & Check Redis Rate Limiter
    GW->>OS: Forward Request with X-Trace-Id
    OS->>OS: Check Redis Idempotency Key

    rect rgb(240, 248, 255)
        note over OS,DB: Atomic Database Transaction
        OS->>DB: INSERT INTO FIN_ORDERS (status='PAYMENT_PENDING')
        OS->>S3: Upload Invoice Receipt (PDF/TXT)
        OS->>DB: INSERT INTO FIN_OUTBOX_EVENTS (type='ORDER_CREATED')
    end

    alt Synchronous Fast-Path via gRPC
        OS->>PS: gRPC ProcessPayment(orderId, amount, idempotencyKey)
        PS-->>OS: PaymentResponse(status='SUCCESS', paymentId)
        OS->>DB: UPDATE FIN_ORDERS SET status='PAID'
        OS-->>GW: 201 Created (OrderResponse + MinIO URL)
        GW-->>Client: 201 Created
    else Asynchronous / Failure Path (Saga Compensation)
        OS->>PS: gRPC ProcessPayment() Fails or Timeouts
        OS->>DB: Keep Order in PAYMENT_PENDING
        note over OS,MQ: Outbox Poller Worker
        OS->>DB: SELECT pending events
        OS->>MQ: Send OrderEvent to 'fintech.order.events.queue'
        MQ->>PS: Consume OrderEvent
        PS->>PS: Risk / AML Evaluation (Amount > $500,000)
        PS->>MQ: Publish PaymentEvent(status='FAILED') to 'fintech.saga.compensation.queue'
        MQ->>OS: Consume Compensation Event
        OS->>DB: UPDATE FIN_ORDERS SET status='CANCELLED'
    end
```

---

## 3. Distributed Tracing & Centralized Logging Flow

Every HTTP request entering the platform is assigned a unique `X-Trace-Id` (or adheres to W3C `traceparent` headers):
1. **API Gateway**: Generates or forwards `traceId` and propagates via HTTP headers.
2. **Micrometer Tracing + Brave**: Injects `traceId` and `spanId` into Slf4j MDC.
3. **Loki4j Appender**: Ships structured logs with trace metadata directly to Grafana Loki (`http://localhost:3100/loki/api/v1/push`).
4. **Grafana Trace-to-Log Correlation**: Engineers can jump directly from a slow Zipkin span into the exact Loki logs emitted by `api-gateway`, `order-service`, or `payment-service`.

---

## 4. Technology Stack Matrix

| Technology | Role in Architecture | Senior Architectural Decision / Justification |
| :--- | :--- | :--- |
| **Java 17 / 21** | Primary Language Baseline | LTS release offering records, sealed interfaces, enhanced GC, and modern JVM performance required by Spring Boot 3.x. |
| **Java 8** | Legacy Module Target | Showcases legacy system integration, backward-compatibility bridges, and migration mastery. |
| **Spring Boot 3.3.x** | Application Framework | Jakarta EE 10 baseline, native observability with Micrometer, robust security integration. |
| **Spring Cloud Gateway** | API Gateway & Edge Security | Reactive non-blocking I/O (Project Reactor/Netty), centralized OAuth2 resource server, distributed rate limiting. |
| **gRPC & Protobuf** | Synchronous Inter-Service IPC | Binary serialization over HTTP/2, schema-enforced contracts (`payment.proto`), low latency compared to REST. |
| **ActiveMQ (Artemis)** | Asynchronous Messaging & Saga | Enterprise JMS 2.0 broker for resilient, decoupled asynchronous saga orchestration and outbox event streaming. |
| **Redis Cache** | Distributed Cache & Locking | Fast in-memory store for `@Cacheable` order queries, distributed rate limiting, and idempotency deduplication keys. |
| **Oracle Database** | Relational Ledger Persistence | Enterprise ACID transactions with Sequence generators, indexed query execution, and transactional outbox persistence. |
| **MinIO** | S3-Compatible Object Storage | High-durability distributed storage for invoices and receipts with presigned temporary download URLs. |
| **Grafana Loki** | Centralized Log Aggregation | Label-based log indexing tightly integrated with Grafana, eliminating the operational overhead of full-text Elasticsearch clusters. |
| **Micrometer Tracing** | Distributed Tracing | Standardized vendor-neutral instrumentation exporting W3C spans to Zipkin/Jaeger. |
| **Docker & Kubernetes** | Containerization & Orchestration | Declarative deployments, liveness/readiness probes, HPA, and multi-stage container optimization. |
| **EJB (3.x Stateless)** | Legacy Core Banking Interop | Remote Stateless Session Beans (`AccountPostingEJB`) for WebLogic Core Banking ledger settlement. |
| **SOA / SOAP Web Services** | Legacy Core System Integration | JAX-WS Web Services and XML Anti-Corruption Layer (`LegacyCoreBankingSoaClient`) to communicate with Core Banking SOA Suite. |
| **BDNS** | Banking Domain Name Service | Enterprise intranet split-horizon DNS routing resolving banking services across network zones. |
