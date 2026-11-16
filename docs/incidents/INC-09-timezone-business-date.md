# INC-09: Early-morning transactions settled on the wrong day (JVM default time zone)

| | |
|---|---|
| **Severity** | Sev 2: settlement file does not match the bank's |
| **Detected by** | Daily reconciliation: our totals for D-1 too high, D too low, by the same amount |
| **Code** | [`BuggyBusinessCalendar`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc09_timezone/BuggyBusinessCalendar.java) → [`FixedBusinessCalendar`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc09_timezone/FixedBusinessCalendar.java) |
| **Test** | [`Inc09TimeZoneTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc09TimeZoneTest.java) |

## Symptom

Only transactions between 00:00 and 07:59 Malaysia time were wrong. Started the day the service moved
from a VM to containers. Could not be reproduced on developer laptops.

## Investigation

1. "Wrong only in a time window" + "only in one environment" → time zone.
2. `docker exec app date` → UTC. `java -XshowSettings:properties -version | grep user.timezone` → `UTC`.
   The VM was set to `Asia/Kuala_Lumpur`.
3. The code calls `LocalDate.now()` / `ZoneId.systemDefault()`, which silently uses the host's zone.
   00:30 MYT is 16:30 UTC **the previous day**.

## Root cause

The business rule is "the date in Malaysia", but the code said "the date wherever this JVM runs".
`LocalDate.now()` also hides the clock, so the bug could not be tested.

## Fix

- Explicit business zone (`Asia/Kuala_Lumpur`, configurable as `lab.business-zone`).
- Inject a `java.time.Clock` bean; tests pass `Clock.fixed(...)`.
- Store instants (`TIMESTAMPTZ`) and derive the business date once, at write time.

## Prevention

- Ban `LocalDate.now()` / `new Date()` without a clock in business code (ArchUnit rule or review checklist).
- Tests that use timestamps just after midnight local time.

## Interview angle

Shows you understand `Instant` vs `LocalDate` vs `ZonedDateTime`, and why "works on my machine" bugs
are usually environment differences.
