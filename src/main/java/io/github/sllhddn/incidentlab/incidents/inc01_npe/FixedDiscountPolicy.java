package io.github.sllhddn.incidentlab.incidents.inc01_npe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * INC-01 (fixed). Unknown or missing tiers get no discount and a warning in the log,
 * instead of failing the whole checkout. The tier list lives in one place.
 */
public class FixedDiscountPolicy implements DiscountPolicy {

    private static final Logger LOG = Logger.getLogger(FixedDiscountPolicy.class.getName());

    private static final Map<String, Integer> PERCENT_BY_TIER = Map.of(
            "STANDARD", 0,
            "SILVER", 5,
            "GOLD", 10,
            "PLATINUM", 15);

    @Override
    public BigDecimal discountFor(String tier, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        Integer percent = tier == null ? null : PERCENT_BY_TIER.get(tier);
        if (percent == null) {
            LOG.warning(() -> "No discount configured for tier '" + tier + "', applying 0%");
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
