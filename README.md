# java-incident-lab

![CI](https://github.com/sllhddn/java-incident-lab/actions/workflows/ci.yml/badge.svg)

| [Debugging playbook](docs/DEBUGGING-PLAYBOOK.md) | [Incident write-ups](docs/incidents/) | [Changelog](CHANGELOG.md) |
|---|---|---|

**Ten Java production incidents, reproduced as failing tests, root-caused and fixed.**
Java 21 · Spring Boot 3 · Spring Data JPA / Hibernate 6 · PostgreSQL 16 · Flyway · Testcontainers · JUnit 5 · Docker

Most of my work is production support on a large Java/Spring/Hibernate system: reading stack traces,
tracing data problems back to code, and getting releases out safely. This repo is how I practise and
show that skill on code anyone can run. Each incident has:

- the code **as it shipped** (`Buggy…`) and **as fixed** (`Fixed…`), side by side;
- a test that **reproduces the bug** and a test that **proves the fix**, both run in CI;
- a write-up: symptom, how it was investigated (tools and commands), root cause, fix, prevention.

The running application (an order service) always uses the fixed versions.
*Scenarios, data and logs are fictional.*

## The incidents

| # | Incident | Bug class | Found with | Test technique |
|---|---|---|---|---|
| [01](docs/incidents/INC-01-npe-unknown-tier.md) | Checkout fails for one customer group | NPE from auto-unboxing `Map.get` | Stack trace + helpful NPE message | Unit |
| [02](docs/incidents/INC-02-n-plus-one.md) | Order list takes 9 s | Hibernate N+1 | SQL log, Hibernate statistics | Counts SQL statements on real PostgreSQL |
| [03](docs/incidents/INC-03-transactional-self-invocation.md) | "Atomic" import leaves partial data | `@Transactional` self-invocation | Transaction debug log, call stack | Asserts rows after rollback |
| [04](docs/incidents/INC-04-oversell-lost-update.md) | Flash sale oversold stock | Lost update / race | Interleaving analysis | 8 threads + `CyclicBarrier`, deterministic |
| [05](docs/incidents/INC-05-memory-leak-static-cache.md) | OOM every 3 days | Unbounded static cache | GC log, heap dump, MAT | Cache size and hit count |
| [06](docs/incidents/INC-06-deadlock-lock-ordering.md) | Transfers hang, CPU idle | Lock-ordering deadlock | Thread dump (`jstack`) | `ThreadMXBean.findDeadlockedThreads` |
| [07](docs/incidents/INC-07-money-floating-point.md) | Month-end off by sen | `double` for money | Debugger + hand calculation | 68,000+ split combinations |
| [08](docs/incidents/INC-08-boxed-long-equality.md) | Customers ≥ 128 denied access | `==` on `Long` | Conditional breakpoint | Boundary values 127/128 |
| [09](docs/incidents/INC-09-timezone-business-date.md) | Early-morning txns on wrong day | JVM default time zone | Environment diff | Fixed `Clock` |
| [10](docs/incidents/INC-10-concurrent-modification.md) | Cleanup job dies hourly | `ConcurrentModificationException` | Stack trace, iterator internals | Includes the case that hides it |

## Run it

```bash
mvn verify                      # all tests; needs Docker for Testcontainers
docker compose up --build       # app on :8080, PostgreSQL on :5432, debugger on :5005
```

Then use [`requests.http`](requests.http), or:

```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
     -d '{"customerId":4,"sku":"KB-001","quantity":1}'
curl localhost:8080/api/orders
curl localhost:8080/actuator/threaddump
```

Attach a debugger to `localhost:5005` and step through any incident; the
[playbook](docs/DEBUGGING-PLAYBOOK.md#6-using-the-debugger-well) lists the techniques.

## Design choices

| Choice | Why |
|---|---|
| Buggy and fixed code both in `src/main` | The diff is the lesson; tests keep both honest |
| Test hooks (`setRaceWindow`, `setAfterFirstLock`) | Races and deadlocks reproduce **every** run, not once a week |
| Real PostgreSQL in tests (Testcontainers) | Lazy loading, transactions and row locks behave differently in H2 |
| Flyway owns the schema, Hibernate `validate` | App refuses to start if entities and schema disagree |
| `open-in-view: false` | No lazy loading outside transactions (INC-02 cannot come back silently) |
| Conditional `UPDATE` for stock + `CHECK (stock >= 0)` | Atomic in the database, with a constraint as the last line of defence |
| Heap dump on OOM + GC log to a volume | Evidence survives the restart (INC-05) |
| JDWP port in docker compose only | Remote debugging locally, never in the production image |

## Project layout

```
src/main/java/io/github/sllhddn/incidentlab/
  domain/ repo/ service/ web/        the order service (uses the fixed code)
  incidents/inc01_npe … inc10_cme/   Buggy* and Fixed* for each incident
src/test/java/…/incidents/           Inc01…Inc10 tests: reproduce_* and fixed_*
docs/incidents/                      one write-up per incident
docs/DEBUGGING-PLAYBOOK.md           the general method
```

## Licence

MIT
