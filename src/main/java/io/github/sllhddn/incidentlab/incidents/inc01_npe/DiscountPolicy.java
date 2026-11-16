package io.github.sllhddn.incidentlab.incidents.inc01_npe;

import java.math.BigDecimal;

/** Returns the discount for an order amount, based on the customer's tier. */
public interface DiscountPolicy {

    BigDecimal discountFor(String tier, BigDecimal amount);
}
