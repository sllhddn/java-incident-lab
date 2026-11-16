package io.github.sllhddn.incidentlab.incidents.inc01_npe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * INC-01 (as shipped). Works for every tier that existed when it was written.
 * A later data migration added a PLATINUM tier, {@code Map.get} returned null,
 * and auto-unboxing that null into an {@code int} threw a NullPointerException
 * on every checkout for those customers.
 */
public class BuggyDiscountPolicy implements DiscountPolicy {

    private static final Map<String, Integer> PERCENT_BY_TIER = Map.of(
            "STANDARD", 0,
            "SILVER", 5,
            "GOLD", 10);

    @Override
    public BigDecimal discountFor(String tier, BigDecimal amount) {
        int percent = PERCENT_BY_TIER.get(tier); // BUG: unboxes null for an unknown tier
        return amount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
