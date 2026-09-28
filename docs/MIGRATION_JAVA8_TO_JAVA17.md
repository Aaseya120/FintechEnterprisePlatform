# Enterprise Migration Guide: Java 8 & Spring Boot 2 to Java 17/21 & Spring Boot 3.x

**Author**: Senior Java Architect (8+ Years Experience)  
**Target Audience**: Staff / Senior Software Engineers, Enterprise Architecture Board  
**Scope**: Step-by-step modernization strategy for mission-critical core banking and fintech services.

---

## 1. Executive Summary

Many enterprise financial institutions maintain mission-critical backends developed between 2014 and 2018 on **Java 8** and **Spring Boot 1.5.x / 2.x**. While stable, remaining on Java 8 presents substantial risks:
- Commercial support sunsetting and security vulnerability patch lag.
- Inability to leverage modern reactive pipelines, native compilation, or high-density containers.
- Performance handicaps from legacy garbage collectors (CMS / Parallel GC) compared to G1GC and ZGC.

This project demonstrates the co-existence and upgrade path: maintaining `legacy-core-accounting` on a Java 8 baseline while modernizing microservices to **Spring Boot 3.3.x on Java 17/21**.

---

## 2. Key Language & Syntax Modernization

### 2.1 Domain Models: Boilerplate POJOs vs Java 17 Records
**Java 8 (Requires Lombok or 100+ lines of getters/setters/equals):**
```java
public class PaymentRequestDto {
    private final String orderId;
    private final BigDecimal amount;
    // constructors, getters, hashCode, equals, toString...
}
```

**Java 17 / 21 Records (Concise, Immutable, Data-Oriented):**
```java
public record PaymentRequestDto(
    @NotBlank String orderId,
    @NotNull BigDecimal amount,
    String currency
) {}
```

### 2.2 Switch Expressions & Pattern Matching
**Java 8:**
```java
if (event instanceof OrderEvent) {
    OrderEvent oe = (OrderEvent) event;
    // handle order
} else if (event instanceof PaymentEvent) {
    PaymentEvent pe = (PaymentEvent) event;
    // handle payment
}
```

**Java 17 / 21:**
```java
switch (event) {
    case OrderEvent oe -> processOrderEvent(oe);
    case PaymentEvent pe -> processPaymentEvent(pe);
    default -> log.warn("Unrecognized event type: {}", event.getClass());
}
```

### 2.3 Text Blocks (Multiline JSON / SQL / Protobuf)
Eliminates ugly string concatenations and escaped quotes in test fixtures and SQL queries:
```java
String sql = """
    SELECT o.id, o.amount, o.status 
    FROM FIN_ORDERS o 
    WHERE o.customer_id = :customerId 
    ORDER BY o.created_at DESC
    """;
```

---

## 3. Spring Boot 2.x to Spring Boot 3.x Migration Steps

### 3.1 Jakarta EE 10 Namespace Upgrade (`javax.*` to `jakarta.*`)
Spring Boot 3.x is baseline Java 17 and migrates to Jakarta EE 9/10:
| Old Java 8 / Spring Boot 2 Package | New Spring Boot 3.x Package |
| :--- | :--- |
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.annotation.*` | `jakarta.annotation.*` |

> **Automated Refactoring Tip**: Use OpenRewrite recipe `org.openrewrite.recipe:rewrite-spring:migrate-to-spring-boot-3` to automatically convert 95% of imports across the codebase.

### 3.2 Spring Security 6.x Component-Based Architecture
Spring Boot 2 deprecated `WebSecurityConfigurerAdapter`. Spring Boot 3 removes it completely.
**Modern Spring Security 6 Configuration (as implemented in `OrderService`):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 4. Oracle Database Driver Upgrade

In Java 8, legacy banking systems typically relied on `ojdbc6.jar` or `ojdbc7.jar`.  
For Java 17 / Spring Boot 3.x, upgrade to **`ojdbc8:19.23.0.0`** or **`ojdbc11:21.x`**:
```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>19.23.0.0</version>
</dependency>
```
Key advantages:
- Full support for `java.time.*` (`Instant`, `LocalDateTime`, `LocalDate`) mapped directly to Oracle `TIMESTAMP WITH TIME ZONE` and `DATE` columns.
- Non-blocking network I/O enhancements.
- Native Universal Connection Pool (UCP) and HikariCP compatibility.

---

## 5. Garbage Collection & JVM Performance Tuning

In Java 8, production containers often suffered from long Stop-The-World (STW) pauses due to the Concurrent Mark Sweep (CMS) collector.

### Container JVM Flags for Java 17 (G1GC):
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1ReservePercent=15 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:+UseStringDeduplication \
     -XX:MaxRAMPercentage=75.0 \
     -jar app.jar
```
For ultra-low latency requirements (e.g., sub-millisecond payment processing), migrate to **ZGC** (`-XX:+UseZGC`).

---

## 6. Strangler Fig Migration: WebLogic EJB & SOA to Spring Boot Microservices

To modernize a monolithic core banking platform running on **Oracle WebLogic Server (EJB, JMS, SOA Suite)** without catastrophic big-bang cutovers, the platform adopts the **Strangler Fig Pattern** facilitated by an **Anti-Corruption Layer (ACL)**:

```
[ Modern Clients ]
       │ (REST / OAuth2)
       ▼
[ Spring Cloud Gateway ]
       │
       ├─► [ Order Service (Spring Boot 3.x) ]
       │         │ (gRPC HTTP/2)
       │         ▼
       └─► [ Payment Service (Spring Boot 3.x) ]
                 │
                 ├──► [ LegacyCoreBankingSoaClient ] ──► (SOAP/XML) ──► [ WebLogic SOA Suite ]
                 │                                                          (CBS Posting)
                 │
                 └──► [ LegacyCoreBankingEjbBridge ] ──► (RMI/IIOP) ──► [ WebLogic EJB Cluster ]
                                                                            (Stateless Session Bean)
```

### Migration Execution Plan:
1. **Maintain Contracts in `legacy-core-accounting`**:
   - Remote EJB 3.x interface `AccountPostingRemote` and Stateless Session Bean `AccountPostingEJB` preserve core banking calculation logic on a Java 8 baseline.
   - JAX-WS Web Service `CoreBankingSoaService` defines SOAP XML contracts (`SoaPostingRequest` / `SoaPostingResponse`).
2. **Deploy Anti-Corruption Layer (ACL) in Microservices**:
   - `LegacyCoreBankingSoaClient` bridges modern gRPC payments to the WebLogic SOA endpoint (`cbs-weblogic.fintech.enterprise.internal:7001`).
   - `LegacyCoreBankingEjbBridge` abstracts remote EJB JNDI invocations, providing automatic in-process fallback for cloud containers and local testing.
3. **Enterprise BDNS Resolution**:
   - `BdnsServiceResolver` dynamically routes internal banking queries to either on-prem WebLogic or containerized microservice endpoints.
4. **Phased Decommissioning**:
   - Workloads are progressively shifted from WebLogic EJBs to Spring Boot microservices while ensuring continuous double-entry ledger reconciliation.
