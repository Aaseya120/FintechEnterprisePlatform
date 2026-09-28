# B V. REDDY
**Senior Java Developer / Microservices Technical Lead**  
📍 Hyderabad, India | 📞 +91 990821568544 | ✉️ [vjavatech@gmail.com](mailto:vjavatech@gmail.com)  
**Notice Period:** Immediate Joiner (Available to Join Immediately) | **Experience:** 8 Years (2018 – 2026)

---

## PROFESSIONAL SUMMARY
* **Senior Java / Microservices Engineer** with **8 years** of extensive experience (2018 – 2026) architecting, building, and deploying mission-critical, enterprise-grade **Digital Banking and FinTech platforms** for **Boubyan Bank Kuwait** (both onsite in Kuwait City and offshore remotely) under **Aaseya IT Services Pvt. Ltd., Hyderabad**.
* Deep technical expertise in **Java (8, 11, 17, 21)**, **Spring Boot 3.x**, **Spring Cloud Gateway**, **gRPC / Protocol Buffers (HTTP/2)**, **Event-Driven Microservices**, and **Distributed Transaction Orchestration (Saga Pattern & Transactional Outbox)**.
* Proven track record in solving distributed systems challenges: eliminating dual-write anomalies using **Transactional Outbox with ActiveMQ Artemis/JMS**, implementing **Distributed Rate Limiting via Reactive Redis**, and designing clean, decoupled domain-driven architectures.
* Specialized in zero-trust edge security (**OAuth2, JWT, Spring Security Reactive**), high-throughput persistence (**Oracle 19c/21c, Spring Data JPA, Hibernate, Connection Pooling**), and tamper-evident storage (**MinIO S3**).
* Hands-on leader in DevOps and Observability: multi-stage **Docker**, **Kubernetes (K8s)** orchestration, **W3C Distributed Tracing (Micrometer, Zipkin)**, **Grafana Loki** structured telemetry, and automated CI/CD pipelines (**Jenkins, GitLab CI, SonarQube**).

---

## TECHNICAL SKILLS MATRIX

| Category | Technologies & Tools |
| :--- | :--- |
| **Core & Enterprise Java** | Java 21, Java 17, Java 11, Java 8 (Virtual Threads, Records, Pattern Matching, Stream API, Lambdas, Concurrency, Generics, Memory Model/GC Tuning) |
| **Frameworks & Architecture** | Spring Boot 3.3.x, Spring Cloud (Gateway, Config), Spring Data JPA, Hibernate, Spring Security Reactive, RESTful Web Services |
| **Inter-Service IPC & gRPC** | gRPC (Blocking & Streaming Stubs), Protocol Buffers (Protobuf v3), HTTP/2 Multiplexing, Client Deadlines & Circuit Breaking |
| **Messaging & Distributed Sagas** | ActiveMQ Artemis, JMS, Apache Kafka, Transactional Outbox Pattern, Saga Orchestration & Compensating Transactions |
| **Caching & In-Memory Data** | Redis (Reactive Redis, Distributed Rate Limiting, Idempotency Locking, Multi-Level Cache Management) |
| **Object Modeling & Serialization** | Clean Entity/DTO Domain Modeling, Jackson JSON, Protobuf Serialization |
| **Databases & Storage** | Oracle Database (19c/21c), H2 (Oracle Compatibility Mode), PL/SQL, Indexes & Query Optimization, MinIO S3 Object Storage |
| **Observability & Telemetry** | Micrometer Tracing (Brave bridge), Zipkin (W3C traceparent), Grafana Loki, Prometheus, Spring Boot Actuator, Logback |
| **DevOps, Containers & Cloud** | Kubernetes (Deployments, Services, ConfigMaps, Probes, HPA), Docker (Multi-stage builds), Helm, Linux/Unix Shell Scripting |
| **CI/CD & Code Quality** | Jenkins Pipeline as Code, GitLab CI, GitHub Actions, SonarQube, JaCoCo Code Coverage, Maven |
| **Design & Methodologies** | Domain-Driven Design (DDD), Clean Architecture, Microservices Reference Architecture, TDD, Agile/Scrum |

---

## PROFESSIONAL EXPERIENCE

### **Aaseya IT Services Pvt. Ltd., Hyderabad**  
**Role:** Senior Java Developer / Technical Lead  
**Duration:** June 2018 – Present (8 Years)  
**Client:** **Boubyan Bank, Kuwait** (Flagship Digital Islamic Banking & Wealth Platform)  
**Deployment Model:** Onsite (Kuwait City) & Remote Offshore (Hyderabad)

#### **Project 1: Enterprise Microservices Platform & Financial Order/Payment Orchestrator (2022 – 2026)**
* **Role & Scope:** Led the backend architecture and engineering for Boubyan Bank’s core microservices platform handling digital orders, real-time bill payments, payment settlements, and audit invoice generation.
* **Architecture & IPC:**
  * Architected a distributed microservices ecosystem on **Spring Boot 3.3.x** and **Java 21**, establishing strict boundary contracts via a central contracts module (`common-contracts`).
  * Engineered high-speed synchronous inter-service communication between `order-service` and `payment-service` utilizing **gRPC over HTTP/2** with **Protocol Buffers**, slashing binary payload sizes by 65% and reducing inter-service latency to under 25ms.
  * Designed resilience protocols around gRPC client invocations with explicit deadlines, fallback paths, and transactional logging.
* **Distributed Transactions & Saga Management:**
  * Solved the distributed dual-write inconsistency problem by implementing the **Transactional Outbox Pattern** (`FIN_OUTBOX_EVENTS`), ensuring atomic persistence of business state and outbox events in a single local ACID transaction.
  * Built an asynchronous, resilient event publisher job polling pending outbox events and dispatching messages to **ActiveMQ Artemis** queues with guaranteed at-least-once delivery semantics.
  * Implemented **Saga Compensating Transactions** for high-value operations exceeding compliance limits (AML limits > 500,000 KWD), automatically dispatching compensation reversal events across JMS queues and marking orders as `CANCELLED` without distributed lock contention.
* **Edge Routing, Rate Limiting & Security:**
  * Implemented an enterprise **Spring Cloud Gateway** cluster running on the Netty reactive engine, handling centralized routing across banking domain names (BDNS).
  * Built distributed token-bucket rate limiting using **Spring Data Redis Reactive** (`RequestRateLimiter`), shielding downstream microservices from traffic surges and denial-of-service attempts.
* **Tamper-Evident Document Storage & Audit Receipts:**
  * Integrated **MinIO S3 Object Storage** to store immutable, digitally generated order receipts and payment invoices with automated URL generation, adhering to Central Bank of Kuwait (CBK) financial data retention policies.
* **End-to-End Observability:**
  * Built complete distributed tracing across API Gateway, Order Service, and Payment Service using **Micrometer Tracing (Brave)** and **Zipkin**, propagating W3C `traceparent` headers through HTTP, gRPC metadata, and JMS message properties.
  * Configured centralized structured JSON logging pushing directly to **Grafana Loki**, enabling real-time trace correlation and reducing Production incident MTTR by 45%.

---

#### **Project 2: Legacy Core Accounting Engine Modernization (2020 – 2022)**
* **Role & Scope:** Modernized Boubyan Bank's legacy ledger and accounting balance calculation modules from legacy Java 8 to modern Java 17/21 modular architecture.
* **Legacy Modernization & Optimization:**
  * Modernized legacy double-entry bookkeeping and tax calculation engines, replacing imperative loop computations with **Java Stream API**, lambdas, and immutable domain records.
  * Maintained 100% computational equivalence and zero regression across multi-currency decimal calculations using rigorous unit and regression suites.
  * Configured Maven cross-compilation with `--release` flags, enabling clean compilation and backward binary compatibility across multi-version JRE environments.
* **Database & Caching Optimization:**
  * Optimized **Oracle Database** query execution plans, refined B-tree composite indexing on transaction tables, and tuned HikariCP connection pools to comfortably support 4,500+ TPS during peak salary credit windows.
  * Implemented **Redis Distributed Caching** with `@Cacheable` and `@CachePut` abstractions, decreasing database read load by 60% for high-frequency account balance queries.

---

#### **Project 3: Digital Channels Integration & Payment Gateway APIs (2018 – 2020)**
* **Role & Scope:** Developed secure RESTful integration services connecting Boubyan Bank’s mobile/online banking channels with regional payment gateways (KNET, GCC RTGS, SWIFT).
* **Key Achievements:**
  * Implemented distributed idempotency control in **Redis** with TTL locks, completely preventing double-charge scenarios caused by mobile network retries.
  * Designed automated CI/CD deployment pipelines using **Jenkins**, **Docker**, and **SonarQube**, enforcing strict quality gates (zero blocker/critical bugs, >85% JaCoCo code coverage).
  * Participated in on-call production support rotations, triaging and resolving critical banking incidents within strict 15-minute SLA windows.

---

## KEY ARCHITECTURAL CONTRIBUTIONS

1. **Dual-Write Eliminator:** Replaced fragile 2-Phase Commit (XA) with Transactional Outbox + ActiveMQ Artemis, eliminating database deadlocks and distributed locking overhead.
2. **Sub-30ms Payment Pipeline:** Architected binary gRPC inter-service communication between Order and Payment domains, reducing network serialization overhead by 65%.
3. **Enterprise Zero-Trust Gateway:** Deployed Spring Cloud Gateway with reactive Redis rate limiting and JWT validation, protecting backend microservices against distributed spikes.
4. **Resilient Saga Compensator:** Engineered asynchronous compensating transaction workflows that automatically rollback failed distributed state across autonomous databases.
5. **Clean Architecture & Decoupled Modeling:** Architected decoupled domain transfer and persistence layers, ensuring clean code readability and zero-defect data integrity under heavy concurrent write loads.

---

## EDUCATION & CREDENTIALS

* **Bachelor of Technology (B.Tech) in Computer Science & Engineering**
* **Technical Training & Certifications:**
  * Advanced Microservices Architecture with Spring Boot & Spring Cloud
  * Oracle Certified Professional: Java SE Developer
  * Kubernetes Application Developer & Cloud-Native Architecture

---

## PERSONAL DETAILS
* **Full Name:** B V. REDDY
* **Phone:** +91 990821568544
* **Email:** vjavatech@gmail.com
* **Current Location:** Hyderabad, Telangana, India
* **Languages:** English, Telugu, Hindi
* **Availability:** **Immediately Available (0 Days Notice Period)**
