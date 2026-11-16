# INC-07: Month-end reconciliation off by a few ringgit (double for money)

| | |
|---|---|
| **Severity** | Sev 3: finance cannot close the month |
| **Detected by** | Reconciliation report: instalments billed ≠ plan totals, always short by sen |
| **Code** | [`BuggyInstalmentPlanner`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc07_money/BuggyInstalmentPlanner.java) → [`FixedInstalmentPlanner`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc07_money/FixedInstalmentPlanner.java) |
| **Test** | [`Inc07MoneyRoundingTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc07MoneyRoundingTest.java) (checks 68,000+ total/month combinations) |

## Symptom

Each plan is short by 0.01-0.02. With thousands of plans a month, the gap reaches a few hundred ringgit.

## Investigation

1. Pick one short plan and recompute by hand: RM 100.00 over 3 months → 33.33 × 3 = 99.99.
2. Breakpoint in `split`: `each = 33.333333333333336` (a `double`). Each instalment is rounded on its own;
   the leftover 0.01 is never assigned to anyone.
3. Search the code base for `double` and `float` in money paths: `grep -rn "double" src/main/java | grep -i -E "amount|price|total"`.

## Root cause

- `double` cannot represent most decimal fractions exactly (`0.1 + 0.2 != 0.3`).
- Rounding each part independently loses the remainder.

## Fix

Work in whole sen with `BigDecimal`, divide with remainder, give the leftover sen to the first instalments:
100.00 / 3 → `[33.34, 33.33, 33.33]`. The test asserts that the parts always add up to the total and
always have 2 decimal places.

## Prevention

- Money is `BigDecimal` (or `long` minor units) end to end, `NUMERIC(12,2)` in the database.
- Construct with `new BigDecimal("0.10")` or `BigDecimal.valueOf`, never `new BigDecimal(0.1)`.
- Compare with `compareTo`, not `equals` (`2.0` vs `2.00` differ in scale).
- Every split/allocation function has a "parts sum to total" test.

## Interview angle

"Why not double for money?" and "how do you split RM 100 three ways?" are common in banking interviews.
