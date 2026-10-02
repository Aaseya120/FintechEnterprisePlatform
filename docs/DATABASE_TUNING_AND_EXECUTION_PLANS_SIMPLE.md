# Database Tuning & Execution Plans – Simple Notes

## 1. Overview

Database tuning helps keep banking APIs fast and avoid slow queries, database contention, and timeouts.

The source covers **Oracle 19c** and **PostgreSQL 16**.

---

## 2. Composite Indexes

### 2.1 Customer Account Lookup

```sql
CREATE INDEX idx_accounts_customer_status
ON accounts (customer_id, status);
```

**Why?**
- `customer_id` is highly selective, so it is the first column.
- `status` helps filter accounts such as `ACTIVE`.
- Helps avoid a full table scan.

### 2.2 Currency and Balance Query

```sql
CREATE INDEX idx_accounts_curr_bal
ON accounts (currency, available_balance);
```

**Why?**
- Helps queries filtering by currency and minimum balance.
- Can use an index range scan instead of scanning the full table.

### 2.3 Outbox Polling

```sql
CREATE INDEX idx_outbox_status_created
ON outbox_events (status, created_at);
```

**Why?**
- The poller looks for `PENDING` events ordered by `created_at`.
- The index quickly finds pending events.

---

## 3. Query Profiling & Execution Plans

### 3.1 PostgreSQL

Use:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, account_number, customer_id, balance,
       available_balance, status
FROM accounts
WHERE customer_id = 'cust_987654'
  AND status = 'ACTIVE';
```

Example result:

```text
Index Scan using idx_accounts_customer_status
Execution Time: 0.057 ms
Buffers: shared hit=3
```

**How to read it:**
- `Index Scan` → index is being used.
- `shared hit=3` → data was already in memory.
- Very low execution time → query is fast.
- No `Seq Scan` → no full table scan.

### 3.2 Oracle 19c

Use:

```sql
EXPLAIN PLAN FOR
SELECT *
FROM accounts
WHERE customer_id = 'cust_987654'
  AND status = 'ACTIVE';

SELECT *
FROM TABLE(
  DBMS_XPLAN.DISPLAY(
    format => 'ALLSTATS LAST +COST +BYTES'
  )
);
```

Important operations:

```text
INDEX RANGE SCAN
TABLE ACCESS BY INDEX ROWID BATCHED
```

**Meaning:**
- `INDEX RANGE SCAN` → Oracle finds matching rows through the index.
- `TABLE ACCESS BY INDEX ROWID BATCHED` → Oracle efficiently fetches the table rows.

---

## 4. HikariCP Connection Pool

Example:

```text
maximum-pool-size: 20
minimum-idle: 5
connection-timeout: 20000ms
leak-detection-threshold: 2000ms
```

For the example 8-vCPU instance:

```text
Pool Size = (CPU Cores × 2) + Effective Spindle Count
          = (8 × 2) + 4
          = 20
```

**Key points:**
- `maximum-pool-size` → maximum DB connections.
- `connection-timeout` → how long to wait for a connection.
- `leak-detection-threshold` → detects connections held too long.

---

## 5. Optimistic vs Pessimistic Locking

| | Optimistic | Pessimistic |
|---|---|---|
| How | Uses `@Version` | Uses `SELECT FOR UPDATE` |
| Lock | No DB lock initially | DB row lock |
| Conflict | Exception, then retry | Other transaction waits |
| Good for | Read-heavy data | Highly contested data |
| Banking example | Account profile update | Balance debit / fund transfer |

### Interview Answer

**Optimistic locking** is useful when conflicts are rare.  
**Pessimistic locking** is useful when multiple transactions may update the same important row, such as an account balance.

