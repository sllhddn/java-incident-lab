package io.github.sllhddn.incidentlab.incidents.inc08_boxing;

import java.util.Objects;

/** INC-08 (fixed). Value comparison, null-safe. */
public class FixedCustomerMatcher implements CustomerMatcher {

    @Override
    public boolean sameCustomer(Long a, Long b) {
        return Objects.equals(a, b);
    }
}
