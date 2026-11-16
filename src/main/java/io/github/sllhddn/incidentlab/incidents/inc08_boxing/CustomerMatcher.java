package io.github.sllhddn.incidentlab.incidents.inc08_boxing;

/** Decides whether two customer ids refer to the same customer. */
public interface CustomerMatcher {

    boolean sameCustomer(Long a, Long b);
}
