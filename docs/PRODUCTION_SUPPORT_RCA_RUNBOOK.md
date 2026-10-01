# Production Support, RCA & Troubleshooting Runbook
**Author:** Senior / Lead Banking Microservices Architect  
**Tools:** Splunk, AppDynamics APM, JCMD, Eclipse MAT, OpenTelemetry, Prometheus

---

## 1. Executive Summary & Incident Triage Workflow
In high-frequency core banking environments, service interruptions directly impact customer transactions and regulatory SLAs. This runbook establishes diagnostic methodologies and Root Cause Analysis (RCA) procedures for four major production failure modes:
1. JVM Memory Leaks & OOM Crashes
2. Thread-Pool Exhaustion & Thread Pinning in Java 21 Virtual Threads
3. Slow APIs & Connection Pool Starvation
4. Network Latency & Intermittent Packet Loss

---

## 2. Incident 1: JVM Heap Memory Leak Root Cause Analysis

### 2.1 Symptoms
- Alert from Prometheus: `jvm_memory_used_bytes / jvm_memory_max_bytes > 90%` for > 10 minutes.
- AppDynamics APM triggers **Major Health Rule Violation**: `JVM Garbage Collection Time > 2000ms/min`.
- Pod restarts with exit code `137` (OOMKilled by Linux cgroup).

### 2.2 Diagnostic Steps
1. **Trigger On-Demand Heap Dump without Pod Restart**:
   ```bash
   kubectl exec -it <pod-name> -n production -- jcmd 1 GC.heap_dump /tmp/heap_dump.hprof
   kubectl cp production/<pod-name>:/tmp/heap_dump.hprof ./heap_dump.hprof
   ```

2. **Eclipse MAT (Memory Analyzer Tool) Analysis**:
   - Open `.hprof` file in Eclipse MAT. Run **Leak Suspects Report**.
   - Check the **Dominator Tree** ordered by **Retained Heap**.

### 2.3 RCA Findings
- **Culprit**: A local static unbounded cache `ConcurrentHashMap<String, AccountMetadata>` was accumulating customer session metadata without TTL or LRU eviction policy. Retained size was 1.4 GB (68% of total heap).
- **Remediation**:
  1. Replaced static in-memory map with **Redis / AWS ElastiCache** using standard `@Cacheable` with explicit 10-minute TTL.
  2. Configured `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/heapdumps`.

---

## 3. Incident 2: Thread-Pool Exhaustion & Virtual Thread Pinning

### 3.1 Symptoms
- Splunk Alert: Spike in HTTP 504 Gateway Timeout from `api-gateway`.
- Tomcat worker threads in `account-service` reach 100% saturation.
- Average response time climbs from 15ms to 12,000ms.

### 3.2 Splunk Query Investigation
```splunk
index="banking_production" sourcetype="spring_boot" app="account-service" 
| search "rejected execution" OR "Timeout waiting for connection" OR "pool-exhausted"
| timechart span=1m count by logger
```

### 3.3 Diagnostic Steps: Inspect Thread Dumps
```bash
# Capture thread dump in modern Java 21
kubectl exec -it <pod-name> -n production -- jcmd 1 Thread.dump_to_file -format=json /tmp/threaddump.json
```

### 3.4 RCA Findings
- **Root Cause (Virtual Thread Pinning)**:
  - While Java 21 Virtual Threads (`threads.virtual.enabled=true`) allow millions of concurrent tasks, several legacy security filters were executing `synchronized (lock)` blocks during blocking I/O calls to LDAP/Keycloak.
  - This caused Virtual Threads to **pin their underlying carrier platform thread (ForkJoinPool worker)**, exhausting the platform thread pool and preventing new virtual threads from executing.
- **Remediation**:
  - Replaced legacy `synchronized` blocks with `java.util.concurrent.locks.ReentrantLock`.
  - Added JVM flag `-Djdk.tracePinnedThreads=full` to CI/CD pipeline tests to catch pinning before production deployment.

---

## 4. Incident 3: Slow APIs & Database Connection Pool Starvation

### 4.1 Symptoms
- AppDynamics APM indicates **Very Slow Transactions** alert on `/api/v1/payments/transfer`.
- Error logs report `HikariPool-1 - Connection is not available, request timed out after 20000ms`.

### 4.2 AppDynamics APM Snapshot Analysis
- Open Transaction Snapshot in AppDynamics:
  - Total transaction time: 20,412 ms
  - Time spent in JDBC call: 20,001 ms (`HikariCP.getConnection()`)
  - Execution thread: `http-nio-8082-exec-14`

### 4.3 Splunk Query for Slow DB Operations
```splunk
index="banking_production" sourcetype="spring_boot" app="payment-service"
| rex "execution time: (?<db_duration_ms>\d+) ms"
| where db_duration_ms > 2000
| stats count, avg(db_duration_ms), max(db_duration_ms) by sql_query
```

### 4.4 RCA Findings
- **Root Cause**: A developer had enclosed a long-running external HTTP third-party fraud check call inside an active `@Transactional` Spring service method. As a result, the database connection from HikariCP was held idle for up to 15 seconds per request.
- **Remediation**:
  - Refactored the workflow so the external HTTP call occurs **before** opening the database transaction.
  - Reduced `@Transactional` scope to only immediate database mutation steps.
  - Configured `spring.datasource.hikari.leak-detection-threshold: 2000`.

---

## 5. Incident 4: Network Latency & Intermittent Packet Drops

### 5.1 Symptoms
- Intermittent HTTP 502 Bad Gateway between `api-gateway` and downstream microservices.
- P99 latency spikes occurring every 15 minutes during high-traffic banking hours.

### 5.2 Splunk Query for Network Latency Breakdown
```splunk
index="banking_production" app="api-gateway"
| eval upstream_latency = upstream_response_time_ms
| where upstream_latency > 3000
| timechart span=5m avg(upstream_latency), perc95(upstream_latency), perc99(upstream_latency) by downstream_service
```

### 5.3 RCA Findings
- AWS VPC Flow Logs revealed that `api-gateway` pods were crossing Availability Zones (AZs) to reach `account-service` pods via an internal load balancer that lacked **Cross-Zone Load Balancing** alignment and EKS **Topology-Aware Routing**.
- **Remediation**:
  - Enabled Kubernetes **Topology Aware Hints** (`topology.kubernetes.io/zone`) to keep traffic within the same AWS Availability Zone.
  - Enabled HTTP Keep-Alive connection pooling with `HttpClient` connection reuse in Spring Cloud Gateway.
  - Decreased p99 cross-AZ network latency from 45ms to 1.2ms.
