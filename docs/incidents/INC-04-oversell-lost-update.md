# INC-04: Flash sale sold more units than existed (lost update)

| | |
|---|---|
| **Severity** | Sev 1: money taken for stock that does not exist |
| **Detected by** | Warehouse: 3 units on the shelf, 8 paid orders |
| **Code** | [`ReadThenWriteStockReservation`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc04_oversell/ReadThenWriteStockReservation.java) → [`AtomicStockReservation`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc04_oversell/AtomicStockReservation.java) |
| **Test** | [`Inc04OversellTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc04OversellTest.java): 8 concurrent buyers, 3 units, real PostgreSQL |

## Symptom

Stock for the item showed **2** after the sale, which looked healthy, yet 8 orders were paid.
No errors in the log. Could not be reproduced by clicking through UAT one request at a time.

## Investigation

1. "Works alone, fails under load, no exception" → suspect a race, not a bug in a single path.
2. Read the code as if two threads run it line by line:
   ```
   T1: read stock = 3          T2: read stock = 3
   T1: 3 >= 1, write 2         T2: 3 >= 1, write 2     <- T1's update is lost
   ```
3. Check the isolation level: PostgreSQL default is READ COMMITTED. Two transactions reading the same
   row at the same time is allowed; `@Transactional` does not serialise them.
4. Reproduce deterministically: the test puts a `CyclicBarrier` between "read" and "write", so all 8
   threads read before any writes. Flaky races become a test that fails every time.

## Root cause

Check-then-act on a shared value across two statements, with the decision made in Java. Classic lost update.

## Fix

Make the check and the change one atomic statement:

```sql
update product set stock = stock - :qty, version = version + 1
 where id = :id and stock >= :qty      -- returns 0 rows if not enough
```

Writers queue on the row lock; after the lock is granted PostgreSQL re-evaluates `stock >= :qty`
against the latest row. The test proves exactly 3 succeed and stock ends at 0.

Alternatives and when to use them:

| Approach | Use when |
|---|---|
| Conditional `UPDATE` (chosen) | Simple counters; highest throughput |
| Optimistic locking (`@Version`) + retry | Complex entity changes; conflicts are rare |
| `SELECT ... FOR UPDATE` (pessimistic) | Must read, compute, then write; short transactions only |
| `CHECK (stock >= 0)` constraint | Always, as the last line of defence |

## Prevention

- The schema has `CHECK (stock >= 0)`.
- Concurrency tests with a barrier for every "reserve / decrement / balance" operation.

## Interview angle

"Two users buy the last item at the same time" → name the race, show the interleaving, give the atomic
update, discuss optimistic vs pessimistic locking and isolation levels.
