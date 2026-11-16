# Java production debugging playbook

The checklist I use, in the order I use it. Each step links to an incident in this repo where it was the step that cracked the case.

## 0. First five minutes

| Question | How |
|---|---|
| What changed? | Last deploy, config change, data migration, traffic spike, infra move ([INC-01](incidents/INC-01-npe-unknown-tier.md), [INC-09](incidents/INC-09-timezone-business-date.md)) |
| Everyone or some? | Compare a failing and a passing request: user, tier, id range, time of day ([INC-08](incidents/INC-08-boxed-long-equality.md)) |
| Error or slow or hung? | Exceptions → step 1. Slow → step 3. Hung with low CPU → step 2 |
| Restore first | Roll back, restart, feature flag off. Then investigate with the evidence you saved (dumps, logs) |

## 1. Reading a stack trace

1. Find the **first frame in your own package**. Skip framework frames (Spring proxies, Tomcat, reflection).
2. Read the exception **message**. JDK 14+ helpful NPE messages name the null expression.
3. Follow every `Caused by:`; the root cause is usually the **last** one.
4. Ask *why* the value is wrong, not just *where*. Reproduce in a unit test before fixing.

## 2. Hung or blocked: thread dumps

```bash
jcmd <pid> Thread.print > td1.txt; sleep 5; jcmd <pid> Thread.print > td2.txt
# or: jstack <pid>    or: curl localhost:8080/actuator/threaddump
grep -A 12 "Found one Java-level deadlock" td1.txt
grep -c "java.lang.Thread.State: BLOCKED" td1.txt
```

- Same threads in the same frames across dumps → stuck, not slow.
- `BLOCKED` on a monitor → lock contention or deadlock ([INC-06](incidents/INC-06-deadlock-lock-ordering.md)).
- Many threads in `HikariPool.getConnection` → pool exhausted: look for long transactions or slow SQL.

## 3. Slow: count before you tune

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE    # bind values (local only)
spring.jpa.properties.hibernate.generate_statistics=true
```

- Many similar queries per request → N+1 ([INC-02](incidents/INC-02-n-plus-one.md)).
- One slow query → `EXPLAIN (ANALYZE, BUFFERS)` in PostgreSQL, `EXPLAIN PLAN` + `DBMS_XPLAN` in Oracle.
- Database side: `pg_stat_statements`, `pg_stat_activity` (who is waiting on whom), `pg_locks`.

## 4. Memory: GC floor, dump, dominator tree

```bash
jstat -gcutil <pid> 5s                      # old-gen floor after full GC rising = leak
jcmd <pid> GC.class_histogram | head -25    # what is filling the heap
jcmd <pid> GC.heap_dump /dumps/now.hprof    # or -XX:+HeapDumpOnOutOfMemoryError
```

Open the `.hprof` in Eclipse MAT → Leak Suspects → Dominator Tree → Path to GC Roots
([INC-05](incidents/INC-05-memory-leak-static-cache.md)).

## 5. Wrong data, no error

- Transactions: is there actually one? `logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG`
  ([INC-03](incidents/INC-03-transactional-self-invocation.md)).
- Concurrency: write the two-thread interleaving on paper; reproduce with a `CyclicBarrier`
  ([INC-04](incidents/INC-04-oversell-lost-update.md)).
- Money: `double` anywhere in the path? Do the parts add up to the total? ([INC-07](incidents/INC-07-money-floating-point.md))
- Time: which zone is the JVM in? `java -XshowSettings:properties -version | grep timezone`
  ([INC-09](incidents/INC-09-timezone-business-date.md)).

## 6. Using the debugger well

`docker compose up` starts the app with JDWP on port 5005.
IntelliJ: *Run → Edit Configurations → + → Remote JVM Debug → localhost:5005*.
VS Code: `"type": "java", "request": "attach", "hostName": "localhost", "port": 5005`.

| Technique | When |
|---|---|
| Conditional breakpoint (`customerId > 127`) | Bug only for some inputs ([INC-08](incidents/INC-08-boxed-long-equality.md)) |
| Exception breakpoint on `NullPointerException` / `ConcurrentModificationException` | Stop exactly where it is thrown ([INC-10](incidents/INC-10-concurrent-modification.md)) |
| Evaluate expression: `TransactionSynchronizationManager.isActualTransactionActive()` | "Is there a transaction here?" |
| Look at the call stack for proxy frames (`TransactionInterceptor`) | Spring AOP not applied |
| Logpoint (non-suspending breakpoint that logs) | Shared environments where pausing is not acceptable |
| Thread view: freeze one thread, step another | Reproduce a race by hand |

Never leave a suspending breakpoint on a shared server: it pauses every request that hits it.

## 7. Close the loop

1. Failing test that reproduces the bug.
2. Fix; the test passes.
3. Write-up: symptom, investigation, root cause, fix, prevention (the format in [`docs/incidents`](incidents/)).
4. One prevention item that would have caught it earlier (test, alert, lint rule, constraint).
