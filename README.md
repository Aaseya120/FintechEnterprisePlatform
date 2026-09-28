# Fintech Enterprise Platform :: Senior Java Developer Reference Architecture

[![Java 17/21](https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg)](https://openjdk.org/)
[![Java 8 Baseline](https://img.shields.io/badge/Java-8%20(Legacy%20Core)-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![gRPC](https://img.shields.io/badge/gRPC-Protobuf%20v1.66-informational.svg)](https://grpc.io/)
[![ActiveMQ](https://img.shields.io/badge/Messaging-ActiveMQ%20Artemis-red.svg)](https://activemq.apache.org/)
[![MinIO S3](https://img.shields.io/badge/Storage-MinIO%20S3-crimson.svg)](https://min.io/)
[![SonarQube Quality Gate](https://img.shields.io/badge/SonarQube-Passed-success.svg)](https://www.sonarqube.org/)
[![Kubernetes](https://img.shields.io/badge/K8s-Production%20Ready-326CE5.svg)](https://kubernetes.io/)

A production-grade **Enterprise Microservices Platform** engineered for a **Senior Java Developer / Microservices Developer with 8+ years of experience**. This project demonstrates enterprise architecture best practices, distributed transactions (Transactional Outbox & Saga pattern), high-performance gRPC inter-service communication, distributed caching, reactive API gateways, centralized telemetry (Loki, Promtail, Zipkin), multi-cloud CI/CD pipelines, and a structured migration bridge from legacy **Java 8** to modern **Spring Boot 3.x on Java 17/21**.

---

## 📑 Complete Technology Coverage Matrix

| Category | Technologies Implemented | Location in Codebase |
| :--- | :--- | :--- |
| **Java Ecosystem** | **Java 8**, **Java 17/21**, **Spring Boot 3.x** | [legacy-core-accounting](file:///d:/Projects/FintechEnterprisePlatform/legacy-core-accounting), [order-service](file:///d:/Projects/FintechEnterprisePlatform/order-service), [payment-service](file:///d:/Projects/FintechEnterprisePlatform/payment-service) |
| **Architecture** | **Microservices**, **Distributed Transactions (Saga / Outbox)** | [docs/ARCHITECTURE.md](file:///d:/Projects/FintechEnterprisePlatform/docs/ARCHITECTURE.md), [docs/DISTRIBUTED_TRANSACTIONS_SAGA.md](file:///d:/Projects/FintechEnterprisePlatform/docs/DISTRIBUTED_TRANSACTIONS_SAGA.md) |
| **Edge & Security** | **Spring Cloud Gateway**, **Spring Security**, **OAuth2**, **JWT** | [api-gateway](file:///d:/Projects/FintechEnterprisePlatform/api-gateway/src/main/java/com/enterprise/fintech/gateway) |
| **Inter-Service IPC** | **gRPC**, **Protocol Buffers (Protobuf)**, **HTTP/2** | [common-contracts](file:///d:/Projects/FintechEnterprisePlatform/common-contracts/src/main/proto/payment.proto), [payment-service](file:///d:/Projects/FintechEnterprisePlatform/payment-service) |
| **Messaging** | **JMS / ActiveMQ Artemis** (Order & Compensation Queues) | [order-service](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/java/com/enterprise/fintech/order/config/JmsConfig.java), [payment-service](file:///d:/Projects/FintechEnterprisePlatform/payment-service/src/main/java/com/enterprise/fintech/payment/listener) |
| **Caching & In-Memory** | **Redis Cache**, Reactive Rate Limiting, Idempotency | [order-service](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/java/com/enterprise/fintech/order/config/RedisConfig.java), [api-gateway](file:///d:/Projects/FintechEnterprisePlatform/api-gateway/src/main/java/com/enterprise/fintech/gateway/config/RateLimiterConfig.java) |
| **Persistence** | **Oracle Database**, **Spring Data JPA**, **H2 In-Memory (Dev)** | [Order.java](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/java/com/enterprise/fintech/order/domain/Order.java), [OutboxEvent.java](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/java/com/enterprise/fintech/order/domain/OutboxEvent.java) |
| **Object Storage** | **MinIO S3** (Receipts, PDF Invoices, Presigned URLs) | [MinioStorageService.java](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/java/com/enterprise/fintech/order/service/MinioStorageService.java) |
| **Observability** | **Grafana Loki**, **Promtail**, **Zipkin**, **Micrometer Distributed Tracing** | [logback-spring.xml](file:///d:/Projects/FintechEnterprisePlatform/order-service/src/main/resources/logback-spring.xml), [docker/loki](file:///d:/Projects/FintechEnterprisePlatform/docker/loki) |
| **Core Banking Bridge** | **EJB 3.x (Stateless Session Beans)**, **SOA / SOAP Web Services**, **BDNS Resolver** | [AccountPostingEJB.java](file:///d:/Projects/FintechEnterprisePlatform/legacy-core-accounting/src/main/java/com/enterprise/fintech/legacy/ejb/AccountPostingEJB.java), [CoreBankingSoaService.java](file:///d:/Projects/FintechEnterprisePlatform/legacy-core-accounting/src/main/java/com/enterprise/fintech/legacy/soa/CoreBankingSoaService.java), [LegacyCoreBankingSoaClient.java](file:///d:/Projects/FintechEnterprisePlatform/payment-service/src/main/java/com/enterprise/fintech/payment/legacy/LegacyCoreBankingSoaClient.java) |
| **Containers & Orchestration** | **Docker**, **Docker Compose**, **Kubernetes (K8s)** | [docker-compose.yml](file:///d:/Projects/FintechEnterprisePlatform/docker/docker-compose.yml), [k8s/](file:///d:/Projects/FintechEnterprisePlatform/k8s) |
| **DevOps & CI/CD** | **GitHub Actions**, **GitLab CI**, **Jenkinsfile**, **SonarQube** | [.github/workflows](file:///d:/Projects/FintechEnterprisePlatform/.github/workflows/ci-cd-pipeline.yml), [.gitlab-ci.yml](file:///d:/Projects/FintechEnterprisePlatform/.gitlab-ci.yml), [Jenkinsfile](file:///d:/Projects/FintechEnterprisePlatform/Jenkinsfile) |
| **Tooling & Enterprise** | **SourceTree**, **Jira**, **Confluence**, **BDNS**, **Postman** | [postman/](file:///d:/Projects/FintechEnterprisePlatform/postman), [docs/JIRA_CONFLUENCE_SPECS.md](file:///d:/Projects/FintechEnterprisePlatform/docs/JIRA_CONFLUENCE_SPECS.md), [docs/BDNS_NETWORKING.md](file:///d:/Projects/FintechEnterprisePlatform/docs/BDNS_NETWORKING.md) |

---

## 🏛️ Project Structure

```
fintech-enterprise-platform/
│
├── pom.xml                               # Root Maven Multi-Module Parent POM
├── sonar-project.properties              # SonarQube Quality Gate configuration
├── Jenkinsfile                           # Declarative Jenkins CI/CD Pipeline
├── .gitlab-ci.yml                        # GitLab CI/CD Pipeline
├── .github/workflows/ci-cd-pipeline.yml  # GitHub Actions Workflow
│
├── common-contracts/                     # Shared gRPC Protobufs & DTOs
│   ├── pom.xml
│   └── src/main/proto/payment.proto      # PaymentService definition (Protobuf 3)
│
├── api-gateway/                          # Spring Cloud Gateway (Reactive, Port 8080)
│   ├── src/main/java/.../gateway/
│   │   ├── config/RateLimiterConfig.java # Redis reactive rate limiter
│   │   ├── filter/TraceIdGlobalFilter.java # Distributed tracing injector
│   │   ├── security/SecurityConfig.java  # OAuth2 Resource Server & JWT
│   │   └── controller/AuthController.java# Postman token generator
│   └── src/main/resources/application.yml
│
├── order-service/                        # Core Order Engine (Spring Boot 3.3.x, Port 8081)
│   ├── src/main/java/.../order/
│   │   ├── domain/                       # Order & OutboxEvent JPA Entities
│   │   ├── repository/                   # Spring Data JPA (Oracle/H2)
│   │   ├── service/OrderService.java     # Saga Orchestrator & Outbox writer
│   │   ├── service/PaymentGrpcClient.java# High-performance gRPC client stub
│   │   ├── service/MinioStorageService.java# MinIO S3 receipt generation
│   │   └── service/OutboxPublisherJob.java # Scheduled ActiveMQ publisher
│   └── src/main/resources/logback-spring.xml # Loki centralized logging
│
├── payment-service/                      # High-speed Payment Engine (Port 8082, gRPC 9090)
│   ├── src/main/java/.../payment/
│   │   ├── grpc/PaymentGrpcServiceImpl.java # gRPC service implementation
│   │   └── listener/PaymentOrderEventListener.java # ActiveMQ listener & Saga compensation
│   └── src/main/resources/application.yml
│
├── legacy-core-accounting/               # Legacy Core Module (Java 8 Baseline)
│   ├── pom.xml                           # Strict Java 8 source/target compliance
│   └── src/main/java/.../legacy/
│       ├── LegacyLedgerEntry.java        # Immutable ledger entity
│       └── LegacyAccountingEngine.java   # Java 8 Stream API & functional calculations
│
├── docker/                               # Container Infrastructure
│   ├── docker-compose.yml                # ActiveMQ, Redis, MinIO, Oracle, Loki, Zipkin
│   ├── Dockerfile.gateway                # Multi-stage Eclipse Temurin JRE 17
│   ├── Dockerfile.order
│   ├── Dockerfile.payment
│   └── loki/ & promtail/                 # Loki logging configuration
│
├── k8s/                                  # Production Kubernetes Manifests
│   ├── namespace.yaml
│   ├── config-and-secrets.yaml
│   ├── api-gateway-deployment.yaml
│   ├── order-service-deployment.yaml
│   ├── payment-service-deployment.yaml
│   └── ingress.yaml
│
├── postman/                              # Automated API Test Suite
│   ├── fintech-platform.postman_collection.json
│   └── fintech-platform.postman_environment.json
│
└── docs/                                 # Senior Architecture & Enterprise Specs
    ├── ARCHITECTURE.md                   # Architecture & Mermaid sequence diagrams
    ├── DISTRIBUTED_TRANSACTIONS_SAGA.md  # Transactional Outbox & Saga Guide
    ├── MIGRATION_JAVA8_TO_JAVA17.md      # Java 8 to 17/21 & Spring Boot 3 migration
    ├── BDNS_NETWORKING.md                # Banking DNS, mTLS & Service Discovery
    ├── JIRA_CONFLUENCE_SPECS.md          # Jira Epics, Stories & Confluence ADRs
    └── SOURCETREE_GIT_WORKFLOW.md        # SourceTree, GitFlow & PR Review standards
```

---

## 🚀 Quickstart Guide

### 1. Launch All Infrastructure (Docker Compose)
Start ActiveMQ Artemis, Redis, MinIO, Grafana Loki, and Zipkin in one command:
```bash
cd docker
docker compose up -d
```
- **ActiveMQ Artemis Web Console**: [http://localhost:8161](http://localhost:8161) (`admin` / `admin`)
- **MinIO Console**: [http://localhost:9001](http://localhost:9001) (`minioadmin` / `minioadmin`)
- **Grafana Loki Logs Dashboard**: [http://localhost:3000](http://localhost:3000) (`admin` / `admin`)
- **Zipkin Distributed Tracing**: [http://localhost:9411](http://localhost:9411)

### 2. Build the Multi-Module Project
```bash
mvn clean install
```
This builds all 5 modules, generates the gRPC protobuf classes (`PaymentServiceGrpc`), runs unit tests, and verifies the Java 8 compliance of `legacy-core-accounting`.

### 3. Run Microservices Locally
In separate terminals:
```bash
# 1. API Gateway (Port 8080)
cd api-gateway && mvn spring-boot:run

# 2. Payment Service (Port 8082, gRPC Port 9090)
cd payment-service && mvn spring-boot:run

# 3. Order Service (Port 8081)
cd order-service && mvn spring-boot:run
```

### 4. Automated Postman Testing
1. Open **Postman**.
2. Click **Import** and select:
   - `postman/fintech-platform.postman_collection.json`
   - `postman/fintech-platform.postman_environment.json`
3. Execute the collection:
   - `1. Generate Access Token` (Acquires JWT bearer token automatically)
   - `2. Create Order` (Triggers DB write, MinIO invoice generation, gRPC payment, and Outbox event)
   - `3. Get Order by Reference` (Validates Redis cache miss/hit)
   - `4. Payment Service Status`
   - `5. Gateway Health`

### 6. Deploy to Kubernetes
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/config-and-secrets.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/ingress.yaml
```

---

## 💡 Key Architectural Highlights for Senior Roles

1. **Transactional Outbox & Saga Orchestration**: Eliminates 2PC distributed deadlocks by persisting events atomically with orders and reliably publishing to ActiveMQ.
2. **Dual-Protocol Microservices**: Exposes HTTP/REST APIs externally through Spring Cloud Gateway while utilizing binary gRPC over HTTP/2 for low-latency inter-service execution.
3. **Enterprise Migration Architecture**: Features an isolated `legacy-core-accounting` module demonstrating how Java 8 codebases (Stream API, functional pipelines, lambdas) are wrapped and migrated into modern Spring Boot 3.x microservices.
4. **Multi-Platform CI/CD & Quality Gates**: Production-grade configurations for GitHub Actions, GitLab CI, and Jenkins, enforcing SonarQube code quality gates (>80% coverage) before container push.
5. **Zero-Trust Enterprise Networking (BDNS)**: Documented Banking Domain Name System architecture for split-horizon DNS, mTLS, and multi-datacenter failover.
