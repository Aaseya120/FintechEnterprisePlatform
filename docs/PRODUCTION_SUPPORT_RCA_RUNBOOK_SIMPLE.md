# Production Support & RCA – Simple Runbook

## 1. Common Production Issues

This runbook covers four common banking microservice problems:

1. JVM memory leak / OOM
2. Thread-pool exhaustion / Virtual Thread pinning
3. Slow API / DB connection pool starvation
4. Network latency / packet loss

Tools used:

- Splunk
- AppDynamics
- JCMD
- Eclipse MAT
- OpenTelemetry
- Prometheus

---

# 2. Incident 1 – JVM Memory Leak / OOM

## Symptoms

- JVM memory stays above 90%.
- Garbage collection becomes high.
- Pod restarts with exit code `137` / `OOMKilled`.

## Diagnosis

Take a heap dump:

```bash
kubectl exec -it <pod-name> -n production -- jcmd 1 GC.heap_dump /tmp/heap_dump.hprof

kubectl cp production/<pod-name>:/tmp/heap_dump.hprof ./heap_dump.hprof
```

Open the dump in **Eclipse MAT** and check:

- Leak Suspects Report
- Dominator Tree
- Retained Heap

## RCA

**Root cause:** An unbounded static `ConcurrentHashMap` kept customer session metadata without TTL or eviction.

## Fix

- Move the cache to Redis / AWS ElastiCache.
- Use `@Cacheable` with a 10-minute TTL.
- Enable automatic heap dumps:

```text
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heapdumps
```

### Interview Answer

> Memory kept increasing because an in-memory cache had no eviction policy. I identified it using a heap dump and Eclipse MAT. We moved the cache to Redis with TTL and enabled automatic heap dumps.

---

# 3. Incident 2 – Thread Pool Exhaustion

## Symptoms

- HTTP `504` errors.
- Tomcat threads reach 100%.
- API response time increases from milliseconds to seconds.

## Splunk Check

```splunk
index="banking_production" sourcetype="spring_boot" app="account-service"
| search "rejected execution" OR "Timeout waiting for connection" OR "pool-exhausted"
| timechart span=1m count by logger
```

## Thread Dump

```bash
kubectl exec -it <pod-name> -n production -- jcmd 1 Thread.dump_to_file -format=json /tmp/threaddump.json
```

## RCA

Legacy code used:

```java
synchronized (lock) {
    // blocking I/O
}
```

With Java 21 Virtual Threads, blocking work inside synchronized code can **pin the carrier thread**.

## Fix

- Replace problematic `synchronized` sections with `ReentrantLock`.
- Test for pinned threads using:

```text
-Djdk.tracePinnedThreads=full
```

### Interview Answer

> The issue was Virtual Thread pinning caused by blocking I/O inside synchronized code. This exhausted carrier threads. We replaced the locking approach and added pinned-thread detection in testing.

---

# 4. Incident 3 – Slow API / HikariCP Starvation

## Symptoms

- Payment API becomes very slow.
- HikariCP reports:

```text
Connection is not available, request timed out after 20000ms
```

## APM Finding

Example:

```text
Total transaction time: 20,412 ms
JDBC / getConnection(): 20,001 ms
```

## Splunk Query

```splunk
index="banking_production" sourcetype="spring_boot" app="payment-service"
| rex "execution time: (?<db_duration_ms>\d+) ms"
| where db_duration_ms > 2000
| stats count, avg(db_duration_ms), max(db_duration_ms) by sql_query
```

## RCA

A long external fraud-check HTTP call was running **inside `@Transactional`**.

So the DB connection stayed occupied while waiting for the external service.

## Fix

- Call the external service before opening the DB transaction.
- Keep `@Transactional` only around DB updates.
- Enable Hikari leak detection:

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 2000
```

### Interview Answer

> The DB connection pool was exhausted because a long external HTTP call was running inside a transaction. We moved the external call outside the transaction and reduced the transaction scope.

---

# 5. Incident 4 – Network Latency

## Symptoms

- Intermittent HTTP `502`.
- P99 latency increases during high traffic.

## Splunk Query

```splunk
index="banking_production" app="api-gateway"
| eval upstream_latency = upstream_response_time_ms
| where upstream_latency > 3000
| timechart span=5m avg(upstream_latency),
  perc95(upstream_latency),
  perc99(upstream_latency) by downstream_service
```

## RCA

AWS VPC Flow Logs showed that gateway traffic was crossing Availability Zones to reach downstream pods.

The setup was not using proper topology-aware routing.

## Fix

- Enable Kubernetes **Topology Aware Hints**.
- Keep traffic within the same AWS Availability Zone where possible.
- Use HTTP Keep-Alive / connection reuse.

### Interview Answer

> Network latency was caused by unnecessary cross-AZ traffic. We used VPC Flow Logs to identify it and enabled topology-aware routing and HTTP connection reuse.

---

# 6. Simple RCA Process

For any production issue:

1. **Identify** the alert and affected service.
2. **Check logs** in Splunk.
3. **Check metrics** in Prometheus.
4. **Check traces/APM** in AppDynamics or OpenTelemetry.
5. **Find the root cause.**
6. **Apply the fix.**
7. **Monitor after the fix.**
8. **Document the RCA and prevention steps.**
