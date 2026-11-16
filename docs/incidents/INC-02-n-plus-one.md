# INC-02: Order list page takes 9 seconds (N+1 queries)

| | |
|---|---|
| **Severity** | Sev 3: slow page, DB CPU at 85% during office hours |
| **Detected by** | p95 latency alert on `GET /api/orders`; DBA reports thousands of identical `SELECT ... FROM order_line WHERE order_id = ?` per minute |
| **Code** | [`OrderListingService`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc02_nplusone/OrderListingService.java), [`OrderRepository.findAllWithDetails`](../../src/main/java/io/github/sllhddn/incidentlab/repo/OrderRepository.java) |
| **Test** | [`Inc02NPlusOneTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc02NPlusOneTest.java) (counts SQL statements with Hibernate statistics) |

## Symptom

Fast in UAT (40 orders), slow in production (40,000). Latency grows linearly with the number of orders.
`pg_stat_statements` top entries:

```
calls   | query
--------+-----------------------------------------------------------
 912344 | select ... from order_line l1_0 where l1_0.order_id=$1
 403118 | select ... from product p1_0 where p1_0.id=$1
   2210 | select ... from customer_order o1_0
```

## Investigation

1. Turn on SQL logging locally (`spring.jpa.show-sql=true` or `logging.level.org.hibernate.SQL=DEBUG`)
   and call the endpoint once: one `select ... from customer_order`, then one `select ... from order_line`
   **per order**. That is the N+1 pattern.
2. Count instead of eyeballing: enable `hibernate.generate_statistics` and read
   `Statistics.getPrepareStatementCount()`. The test does exactly this.
3. Find where the extra queries come from: put a breakpoint in `OrderSummary.of` and step over
   `o.getLines()`: each step fires a query. The associations are `LAZY` (correct), but the code
   walks them one order at a time.

## Root cause

`findAll()` loads only the orders. Building the response touches `customer`, `lines` and each line's
`product`, and Hibernate loads each lazy association with its own query.

## Fix

```java
@EntityGraph(attributePaths = {"customer", "lines", "lines.product"})
@Query("select o from CustomerOrder o order by o.id")
List<CustomerOrder> findAllWithDetails();
```

Result: **1 statement** regardless of row count (the test asserts it). Also kept: `open-in-view: false`,
so lazy loading cannot silently happen in the controller or JSON serializer.

## Prevention

- A test that asserts the statement count for every list endpoint.
- `spring.jpa.open-in-view=false` everywhere.
- For real pagination over collections, use two queries (page of ids, then fetch by ids) instead of a
  collection fetch join with `LIMIT`, which Hibernate would apply in memory.

## Interview angle

"An endpoint got slow as data grew" → think query count first, not query speed. Show how to measure it.
