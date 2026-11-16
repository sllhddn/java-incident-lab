# INC-01: Checkout fails for one group of customers (NullPointerException)

| | |
|---|---|
| **Severity** | Sev 2: one customer segment cannot check out |
| **Detected by** | Error-rate alert on `POST /api/orders`; helpdesk tickets from "premium" customers |
| **Code** | [`BuggyDiscountPolicy`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc01_npe/BuggyDiscountPolicy.java) → [`FixedDiscountPolicy`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc01_npe/FixedDiscountPolicy.java) |
| **Test** | [`Inc01NullPointerTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc01NullPointerTest.java) |

## Symptom

```
ERROR o.a.c.c.C.[dispatcherServlet] Servlet.service() threw exception
java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because the return value
    of "java.util.Map.get(Object)" is null
    at i.g.s.incidentlab.incidents.inc01_npe.BuggyDiscountPolicy.discountFor(BuggyDiscountPolicy.java:22)
    at i.g.s.incidentlab.service.OrderService.placeOrder(OrderService.java:65)
    at jdk.internal.reflect...                       <- 40 lines of Spring/CGLIB proxy frames
```

## Investigation

1. **Read the stack trace bottom-up only until you reach our code.** The first frame in our package is
   `BuggyDiscountPolicy.java:22`. Everything above it is framework.
2. **Read the helpful NPE message** (JDK 14+): it says *which* expression was null, `Map.get(Object)`,
   and that the null was being turned into an `int` (`intValue()`), i.e. auto-unboxing.
3. Only some customers fail → compare a failing and a passing customer. The difference is `tier`.
4. `SELECT tier, count(*) FROM customer GROUP BY tier;` shows a new value, `PLATINUM`, added by a
   marketing data migration two days earlier. The map in code only knows STANDARD/SILVER/GOLD.
5. Reproduced in a unit test before touching the code.

## Root cause

`int percent = map.get(tier);` unboxes the `Integer` result. For a key that is not in the map,
`get` returns `null`, and unboxing `null` throws. The data changed; the code assumed it never would.

## Fix

```diff
- int percent = PERCENT_BY_TIER.get(tier);
+ Integer percent = tier == null ? null : PERCENT_BY_TIER.get(tier);
+ if (percent == null) {
+     LOG.warning(() -> "No discount configured for tier '" + tier + "', applying 0%");
+     return BigDecimal.ZERO.setScale(2);
+ }
```

Plus the missing `PLATINUM` entry. Unknown tiers now degrade (0% and a warning) instead of blocking payment.

## Prevention

- Treat values that come from data (tiers, status codes, currency codes) as open sets: always handle "unknown".
- Long term: a `tier` lookup table with a foreign key, so new tiers need a matching discount row.
- Static analysis (SpotBugs / IntelliJ inspections) flags unboxing of possibly-null `Map.get` results.

## Interview angle

"How do you debug a NullPointerException in production?" → first frame in your own package, the helpful
NPE message, then ask *why* the value is null (here: data changed), reproduce in a test, fix, add a guard.
