# INC-06: Transfers hang with 0% CPU (deadlock)

| | |
|---|---|
| **Severity** | Sev 1: wallet transfers stuck until restart |
| **Detected by** | Requests time out; Tomcat thread pool exhausted; CPU near zero |
| **Code** | [`BuggyTransferService`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc06_deadlock/BuggyTransferService.java) → [`FixedTransferService`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc06_deadlock/FixedTransferService.java) |
| **Test** | [`Inc06DeadlockTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc06DeadlockTest.java) (detects it with `ThreadMXBean.findDeadlockedThreads`, like jstack) |

## Symptom

Hung requests + idle CPU is the signature of threads **waiting**, not working.

## Investigation

1. Take 2-3 thread dumps a few seconds apart: `jstack <pid>`, `jcmd <pid> Thread.print`, or
   `GET /actuator/threaddump`. If the same threads are stuck in the same place in every dump, they are blocked.
2. The JVM detects monitor cycles for you. At the bottom of the dump:

```
Found one Java-level deadlock:
=============================
"pay-b-to-a":
  waiting to lock monitor 0x00007f3c (object 0x000000071a, a ...inc06_deadlock.Account),
  which is held by "pay-a-to-b"
"pay-a-to-b":
  waiting to lock monitor 0x00007f40 (object 0x000000071b, a ...inc06_deadlock.Account),
  which is held by "pay-b-to-a"
```

3. The stacks point at `BuggyTransferService.transfer`: lock `from`, then lock `to`.
   A→B and B→A at the same moment lock in opposite orders.

## Root cause

Lock order depends on the direction of the transfer, so two transfers can each hold one lock and wait
forever for the other. All four Coffman conditions hold; breaking any one prevents deadlock.

## Fix

Impose a global order: always lock the account with the lower id first.

```java
Account first = from.id() < to.id() ? from : to;
Account second = first == from ? to : from;
synchronized (first) { synchronized (second) { ... } }
```

Alternatives: `ReentrantLock.tryLock(timeout)` with back-off, or move the transfer into the database
(`UPDATE ... ORDER BY id FOR UPDATE`) where the DB detects deadlocks and aborts one side.

## Prevention

- One documented lock order for anything that takes more than one lock.
- Concurrency tests that force the bad interleaving (barrier after the first lock) instead of hoping
  a load test hits it.

## Interview angle

"System hangs, CPU is low" → thread dumps, look for BLOCKED threads and "Found one Java-level deadlock",
explain lock ordering.
