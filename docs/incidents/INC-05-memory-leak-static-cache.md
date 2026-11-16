# INC-05: OutOfMemoryError every 3 days (unbounded static cache)

| | |
|---|---|
| **Severity** | Sev 2: pod restarts, failed requests during GC storms |
| **Detected by** | `OOMKilled` / `java.lang.OutOfMemoryError: Java heap space`; GC time > 30% before each crash |
| **Code** | [`BuggyRateCache`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc05_memoryleak/BuggyRateCache.java) → [`FixedRateCache`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc05_memoryleak/FixedRateCache.java) |
| **Test** | [`Inc05MemoryLeakTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc05MemoryLeakTest.java) |

## Symptom

Heap after each full GC (the "floor") rises steadily for days, then the service dies. A restart "fixes" it.

```
[gc] GC(8121) Pause Full (G1 Compaction Pause) 3891M->3870M(4096M) 2.913s
java.lang.OutOfMemoryError: Java heap space
Dumping heap to /dumps/java_pid1.hprof ...
```

## Investigation

1. **A rising floor after full GC = leak**, not just "needs more memory". Confirm with `gc.log` or
   `jstat -gcutil <pid> 5s`.
2. The image runs with `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps`, so there is a dump
   from the moment of failure. Without one: `jcmd <pid> GC.heap_dump /dumps/now.hprof`.
3. Quick look before opening the dump: `jcmd <pid> GC.class_histogram | head -20` showed millions of
   `HashMap$Node`, `String` and `BigDecimal`.
4. Open the `.hprof` in Eclipse MAT → **Leak Suspects** → one `HashMap` retains 2.9 GB.
   **Path to GC roots** → `BuggyRateCache.CACHE` (a `static` field, so it lives as long as the class).
5. Look at the keys: `USD@2026-11-16T01:00:00.001Z`, `USD@2026-11-16T01:00:00.002Z`, … one per request.

## Root cause

Two mistakes together: the key contains the full timestamp (so the cache never hits), and the map is
`static` and unbounded (so it never shrinks).

## Fix

- Key by what the value really depends on: currency + **business date** (rates are published daily).
- Bound the cache (LRU, `maxEntries`) and make it an instance field owned by a Spring bean.
- In a real service: Caffeine with `maximumSize` + `expireAfterWrite`, and cache metrics exported.

Test: 10,000 lookups → 1 entry and 1 backend call; 1,000 different days → capped at 100.

## Prevention

- Every cache needs a bound and an eviction policy, and its size on a dashboard.
- Always run with `HeapDumpOnOutOfMemoryError` and a persistent dump volume.

## Interview angle

"How do you find a memory leak?" → GC floor trend, heap dump on OOM, histogram, MAT dominator tree and
path to GC roots, then fix the owner of the references.
