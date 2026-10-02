# B V REDDY
**Senior Java Developer**  
📌 Hyderabad, India | ☎ +91 99082 15685 | ✉️ [vjavatech@gmail.com](mailto:vjavatech@gmail.com)  
🔗 [https://www.linkedin.com/in/b-v-r-534156179/](https://www.linkedin.com/in/b-v-r-534156179/)  
⚡ **8+ Years’ Experience** | **Immediate Joiner (0 Days’ Notice)** | **Open to Hybrid / Remote Opportunities**

---

## PROFESSIONAL SUMMARY
Senior Java Backend Developer with over 8+ years of hands-on experience in developing, testing, and maintaining enterprise-grade banking and microservices applications. Strong technical proficiency across Java 21/17/8, Spring Boot 3.x, Spring Cloud, RESTful Web Services, Apache Kafka, Oracle 19c, PostgreSQL, and Redis across all phases of the Software Development Life Cycle (SDLC). Practical experience in building event-driven services with Kafka, applying caching with Redis, configuring Resilience4j circuit breakers, and securing REST endpoints using Spring Security and stateless JWT. Proven track record in writing clean code, optimizing SQL queries, writing unit tests with JUnit/Mockito, and working with Docker and Kubernetes in Agile team environments.

---

## CORE TECHNICAL SKILLS

- **Languages:** Java 21 / 17 / 8, SQL, PL/SQL
- **Frameworks:** Spring Boot 3.x/2.x, Spring Cloud Gateway, Spring Data JPA, Hibernate, Spring Batch
- **Messaging & Caching:** Apache Kafka (Producer, Consumer, Error Handlers, DLQ), Redis (Caching, TTLs)
- **Fault Tolerance:** Resilience4j (Circuit Breakers, Retries, Fallbacks)
- **Databases:** Oracle 19c, PostgreSQL, Flyway Migrations
- **Security & APIs:** RESTful APIs, OpenAPI / Swagger, Spring Security, Stateless JWT, RBAC
- **Cloud & DevOps:** AWS (EKS, EC2), Docker, Kubernetes, GitHub Actions, Jenkins, Git, Maven
- **Monitoring & Testing:** Prometheus, Grafana, Splunk, Spring Boot Actuator, JUnit 5, Mockito

---

## PROFESSIONAL EXPERIENCE

### **Aaseya IT Services Pvt. Ltd. (An A YASH Technologies Company) | Hyderabad, India**
**Payroll Employer** — August 2018 – August 2026 (8 Years)  
**Onsite Client Deputation:** **Boubyan Bank, Kuwait** *(Onsite 2022–2025 | Offshore 2018–2022, 2025–2026)*

---

### **Project #1: Boubyan Digital Bank Enterprise Microservices Platform** | *Jan 2022 – Aug 2026*
**Client:** Boubyan Bank, Kuwait *(Subsidiary of National Bank of Kuwait - NBK)*  
**Role:** Senior Java Developer  
**Domain:** Retail Banking, Payments, Accounts & Card Services  
*Digital Banking platform for Boubyan Bank handling online transactions, fund transfers, account inquiries, bill payments, and digital card management.*

#### **Responsibilities & Hands-On Work:**
- Developed backend microservices using **Java 21** and **Spring Boot 3.x** for core banking modules including payment processing, account management, bill payments, and card services.
- Created RESTful APIs following standard HTTP methods and status codes, documenting request/response contracts using **Swagger / OpenAPI 3.0**.
- Configured **Spring Cloud Gateway** routes and filters for request forwarding, rate limiting, and header enrichment.
- Implemented API security using **Spring Security** with stateless **HMAC-SHA256 JWT** authentication, validating tokens and enforcing role-based permissions (RBAC).
- Built Kafka producers and consumers for asynchronous transaction events, configuring topic partitions, serialization, and `@RetryableTopic` for retry handling.
- Implemented an outbox table pattern with a scheduled poller service to reliably publish transaction events to Kafka after database commits.
- Configured **Resilience4j** `@CircuitBreaker` and `@Retry` annotations with fallback methods on inter-service HTTP calls to handle downstream timeouts safely.
- Implemented **Redis caching** (`@Cacheable`, `@CacheEvict`) with defined TTLs for account balances and currency rates to reduce repeated database queries.
- Added idempotency check logic on payment APIs using Redis keys to prevent duplicate transactions caused by user double-clicks or client retries.
- Created multi-stage **Dockerfiles** for containerizing microservices and deployed application pods onto **Kubernetes / AWS EKS**.
- Wrote and maintained automated CI/CD pipeline steps in **GitHub Actions** and **Jenkins** for build validation and running automated tests.
- Wrote Spring Data JPA entities, custom repository queries, and created **Flyway** migration scripts for Oracle 19c and PostgreSQL database changes.
- Configured **Spring Boot Actuator** health endpoints and checked application metrics in **Prometheus/Grafana** and server logs in **Splunk** during production deployments.
- Wrote unit and integration test cases using **JUnit 5** and **Mockito**, maintaining code quality and resolving SonarQube code smells.

**Technologies:**  
Java 21/17, Spring Boot 3.x, Spring Cloud Gateway, Spring Data JPA, Apache Kafka, Redis, Oracle 19c, PostgreSQL, Spring Security, JWT, Resilience4j, Docker, Kubernetes (AWS EKS), GitHub Actions, Jenkins, Prometheus, Grafana, Splunk, JUnit 5, Mockito, OpenAPI/Swagger.

---

### **Project #2: Boubyan Core Digital Banking Integration** | *Aug 2018 – Dec 2021*
**Client:** Boubyan Bank, Kuwait  
**Role:** Java Developer  
**Domain:** Core Banking Middleware, Legacy Mainframe (CBS) Integration & Batch Processing  
*Integration backend responsible for connecting digital banking channels to backend Core Banking Systems (CBS), regional payment gateways, and notification services.*

#### **Responsibilities & Hands-On Work:**
- Developed REST and SOAP/XML service adapters using **Java 8/11** and **Spring Boot** to exchange transaction data with the bank’s legacy Core Banking System (CBS).
- Implemented secure API communication with regional payment network switches (**K-Net**) and formatted financial messages according to bank standards.
- Built **Apache Kafka** consumers for reading audit and alert messages, dispatching SMS and email customer notifications asynchronously.
- Implemented Dead Letter Queue (DLQ) consumer listeners to catch and log failed or invalid messages for manual review.
- Wrote and maintained **Oracle 19c PL/SQL** stored procedures, functions, and views to process transaction history and customer account summaries.
- Implemented **Spring Batch** jobs with chunk-based processing to run nightly reconciliation files and generate monthly account statements.
- Configured **Redis** to cache static reference data (such as bank branch lists and currency codes) to avoid repetitive queries to the legacy host.
- Developed automated test suites using **JUnit** and **Mockito** for testing business logic, validation rules, and error handling.
- Configured Maven build profiles and automated build triggers in **Jenkins** for daily integration builds.
- Used **VisualVM** and **JConsole** to monitor memory usage and thread counts, assisting the team in identifying and fixing unclosed database connections.
- Participated in daily Agile stand-ups, sprint planning, and maintained technical documentation of API mappings in Confluence.

**Technologies:**  
Java 8/11, Spring Boot 2.x, Spring Data JPA, Hibernate, Spring Batch, Oracle 19c, PL/SQL, SOAP/WSDL, Apache Kafka, Redis, Docker, Jenkins, Maven, Git, JUnit, Mockito, VisualVM, Confluence.

---

## KEY ACHIEVEMENTS
- Successfully delivered and maintained core payment and account microservices in production with zero high-severity transaction bugs.
- Reduced database query latency on high-frequency account inquiry endpoints by implementing Redis caching with appropriate TTLs.
- Improved application stability during downstream service outages by configuring Resilience4j circuit breakers and fallbacks.
- Maintained over 80% test coverage across assigned modules using JUnit 5 and Mockito, reducing post-release defect counts.
- Successfully supported production releases and on-call rotations, quickly analyzing logs in Splunk to resolve application issues.

---

## EDUCATION & CERTIFICATIONS
- **Education:** Master of Computer Applications (MCA) — JNTU, Hyderabad, Telangana, India
- **Certifications:** Oracle Certified Professional (OCP): Java SE Programmer
- **Availability:** Immediate Joiner (0 Days’ Notice) | **Work Mode:** Open to Hybrid / Remote | **Location:** Hyderabad, India
