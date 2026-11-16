# INC-10: Expired sessions never cleaned up (ConcurrentModificationException kills a scheduled job)

| | |
|---|---|
| **Severity** | Sev 3: memory growth, stale sessions still valid |
| **Detected by** | Active-session count only ever goes up; one stack trace per hour in the log |
| **Code** | [`BuggySessionReaper`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc10_cme/BuggySessionReaper.java) → [`FixedSessionReaper`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc10_cme/FixedSessionReaper.java) |
| **Test** | [`Inc10ConcurrentModificationTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc10ConcurrentModificationTest.java) |

## Symptom

```
ERROR o.s.s.s.TaskUtils$LoggingErrorHandler : Unexpected error occurred in scheduled task
java.util.ConcurrentModificationException
    at java.base/java.util.ArrayList$Itr.checkForComodification(ArrayList.java:1095)
    at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1049)
    at i.g.s.incidentlab.incidents.inc10_cme.BuggySessionReaper.removeExpired(BuggySessionReaper.java:17)
```

## Investigation

1. The name suggests threads, but a CME is usually **one** thread changing a collection while iterating it.
2. The frame in our code is the `for (Session s : sessions)` line; the loop body calls `sessions.remove(s)`.
3. Why tests passed: removing the second-to-last element ends the loop before the iterator checks
   (`hasNext()` returns false). The unit test happened to use exactly that case. The test in this repo keeps
   both cases to show it.

## Root cause

For-each uses an `Iterator`. Removing through the list (not the iterator) changes `modCount`, and the next
`next()` call fails fast.

## Fix

`sessions.removeIf(s -> s.isExpired(now))` (or `iterator.remove()`). If the registry is shared between request
threads and the job, also use a concurrent structure (`ConcurrentHashMap` keyed by session id) or a lock: the
fail-fast check is best-effort, not a thread-safety guarantee.

## Prevention

- Scheduled jobs should catch, log and **alert** on failure, not just log.
- Test removal at the first, middle and last positions.

## Interview angle

A short, common question. The extra credit is explaining why it sometimes does *not* throw.
