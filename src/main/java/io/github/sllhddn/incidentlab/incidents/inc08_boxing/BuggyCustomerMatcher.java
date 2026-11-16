package io.github.sllhddn.incidentlab.incidents.inc08_boxing;

/**
 * INC-08 (as shipped). Compares boxed {@code Long}s with {@code ==}, which compares
 * object references. {@code Long.valueOf} caches -128..127, so every test with small
 * ids passed; in production, customer 128 and above was never matched to its own orders.
 */
public class BuggyCustomerMatcher implements CustomerMatcher {

    @Override
    public boolean sameCustomer(Long a, Long b) {
        return a == b; // BUG: reference comparison
    }
}
