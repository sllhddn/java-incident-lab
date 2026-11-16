# INC-08: Customers with id ≥ 128 cannot see their own orders (== on Long)

| | |
|---|---|
| **Severity** | Sev 2: authorisation check wrongly denies access |
| **Detected by** | Tickets: "403 on my own order", only from newer customers |
| **Code** | [`BuggyCustomerMatcher`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc08_boxing/BuggyCustomerMatcher.java) → [`FixedCustomerMatcher`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc08_boxing/FixedCustomerMatcher.java) |
| **Test** | [`Inc08BoxingTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc08BoxingTest.java) |

## Symptom

Every test passed (test data used ids 1-10). In production, the first 127 customers were fine and every
customer after that got "access denied" on their own orders.

## Investigation

1. "Fine below a number, broken above it" → look for a boundary. 127/128 is the boundary of the
   `Integer`/`Long` cache.
2. Conditional breakpoint in the check with `customerId > 127`: inspect `a` and `b`. The debugger shows
   the same value, `128`, but different object ids (`Long@1023` vs `Long@1024`).
3. The code is `return a == b;` on `Long`, a reference comparison.

## Root cause

`==` on boxed types compares references. `Long.valueOf(n)` returns cached instances for -128..127, so
`==` happens to work there and fails above it.

## Fix

`Objects.equals(a, b)` (null-safe), or compare primitives `a.longValue() == b.longValue()` after a null check.

## Prevention

- Static analysis: SpotBugs `RC_REF_COMPARISON`, Sonar S4973, IntelliJ "Number comparison using '=='".
- Test data that crosses the boundary (ids > 127).

## Interview angle

The classic `==` vs `equals()` question, with a production consequence attached.
