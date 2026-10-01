# Database Optimization, Query Profiling & Execution-Plan Analysis Guide

## 1. Overview
This architectural guide documents the enterprise database strategies implemented across **Oracle 19c** and **PostgreSQL 16** for the Core Banking Microservices Platform. High-throughput financial platforms process tens of thousands of transactions per second where database contention, suboptimal execution plans, or missing composite indexes can degrade API p99 latency from milliseconds to timeouts.

---

## 2. Composite Index Design & Index Selectivity Analysis

### 2.1 Index 1: Customer Account Dashboard Lookup
```sql
CREATE INDEX idx_accounts_customer_status ON accounts (customer_id, status);
```
- **Rationale**:
  - `customer_id` has high cardinality (hundreds of thousands of unique customers). Placing it as the **leading column** in the B-Tree allows index seek directly into customer partitions.
  - `status` has low cardinality (`ACTIVE`, `DORMANT`, `FROZEN`, `CLOSED`). Appending `status` avoids a secondary heap lookup (table access) when filtering active accounts.
- **Selectivity**:
  $$\text{Selectivity} = \frac{\text{Unique Keys}}{\text{Total Rows}} \approx \frac{500,000}{1,000,000} = 0.5$$

### 2.2 Index 2: Currency-Filtered Liquidity & Minimum Balance Query
```sql
CREATE INDEX idx_accounts_curr_bal ON accounts (currency, available_balance);
```
- **Rationale**:
  - Queries evaluating liquid reserve balances across currencies (`WHERE currency = 'USD' AND available_balance >= 100000 ORDER BY available_balance DESC`) perform an **Index Range Scan** without table scan.

### 2.3 Index 3: Transactional Outbox Polling
```sql
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
```
- **Rationale**:
  - The poller runs every 500ms querying `WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 50`.
  - With this composite index, the database executes an index range scan solely over uncommitted/pending events, completely skipping millions of already published rows.

---

## 3. Query Profiling & Execution Plan Analysis

### 3.1 PostgreSQL Query Plan Analysis (`EXPLAIN (ANALYZE, BUFFERS)`)
```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT id, account_number, customer_id, balance, available_balance, status 
FROM accounts 
WHERE customer_id = 'cust_987654' AND status = 'ACTIVE';
```

#### Execution Plan Output:
```text
Index Scan using idx_accounts_customer_status on public.accounts  (cost=0.42..8.44 rows=2 width=146) (actual time=0.038..0.041 rows=2 loops=1)
  Output: id, account_number, customer_id, balance, available_balance, status
  Index Cond: (((accounts.customer_id)::text = 'cust_987654'::text) AND ((accounts.status)::text = 'ACTIVE'::text))
  Buffers: shared hit=3
Planning Time: 0.089 ms
Execution Time: 0.057 ms
```
- **Analysis**:
  - **Shared Hit = 3**: 0 disk reads; entire B-tree path resolved in shared buffer cache.
  - **Execution Time**: **0.057 ms**, meeting sub-millisecond banking latency SLAs.
  - Absence of `Seq Scan` (Sequential Scan) prevents full-table locks.

---

### 3.2 Oracle 19c Query Profiling & SQL Execution Plan (`DBMS_XPLAN`)
```sql
EXPLAIN PLAN FOR
SELECT * FROM accounts 
WHERE customer_id = 'cust_987654' AND status = 'ACTIVE';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(format => 'ALLSTATS LAST +COST +BYTES'));
```

#### Oracle 19c Plan Output:
```text
-----------------------------------------------------------------------------------------------------------------
| Id  | Operation                           | Name                        | Rows  | Bytes | Cost (%CPU)| Time     |
-----------------------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                    |                             |     2 |   292 |     3   (0)| 00:00:01 |
|   1 |  TABLE ACCESS BY INDEX ROWID BATCHED| ACCOUNTS                    |     2 |   292 |     3   (0)| 00:00:01 |
|*  2 |   INDEX RANGE SCAN                  | IDX_ACCOUNTS_CUSTOMER_STATUS|     2 |       |     2   (0)| 00:00:01 |
-----------------------------------------------------------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------
   2 - access("CUSTOMER_ID"='cust_987654' AND "STATUS"='ACTIVE')
```
- **Analysis**:
  - **Operation**: `INDEX RANGE SCAN` on `IDX_ACCOUNTS_CUSTOMER_STATUS`.
  - **Table Access By Index Rowid Batched**: Oracle batches row ID lookups into contiguous I/O requests.

---

## 4. Connection Pool Optimization (HikariCP)
Formula for optimal pool sizing:
$$\text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Effective Spindle Count}$$

For an 8-vCPU cloud container instance with high-IOPS NVMe EBS storage:
$$\text{Pool Size} = (8 \times 2) + 4 = 20 \text{ connections}$$

- **`maximum-pool-size: 20`**
- **`minimum-idle: 5`**
- **`connection-timeout: 20000ms`** (fails fast rather than cascading thread backlog)
- **`leak-detection-threshold: 2000ms`** (logs stack trace if a transaction holds connection longer than 2 seconds).

---

## 5. Concurrency Control: Optimistic vs. Pessimistic Locking

| Metric | Optimistic Locking (`@Version`) | Pessimistic Write Lock (`SELECT FOR UPDATE`) |
| :--- | :--- | :--- |
| **Mechanism** | Checks version number upon commit | Database row-level exclusive lock |
| **Throughput** | High throughput on read-heavy data | High consistency on contended accounts |
| **Conflict Handling** | `OptimisticLockException` thrown; caller retries | Blocked at DB level until tx completes |
| **Banking Application** | Account profile & contact updates | Core balance debits & fund transfers |
