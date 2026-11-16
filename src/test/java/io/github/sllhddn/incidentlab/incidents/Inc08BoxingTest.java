package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.incidents.inc08_boxing.BuggyCustomerMatcher;
import io.github.sllhddn.incidentlab.incidents.inc08_boxing.FixedCustomerMatcher;
import org.junit.jupiter.api.Test;

class Inc08BoxingTest {

    @Test
    void reproduce_worksUpTo127ThenFails() {
        var buggy = new BuggyCustomerMatcher();

        assertThat(buggy.sameCustomer(127L, 127L)).isTrue();  // Long.valueOf cache: same object
        assertThat(buggy.sameCustomer(128L, 128L)).isFalse(); // two different objects
    }

    @Test
    void fixed_comparesValues() {
        var fixed = new FixedCustomerMatcher();

        assertThat(fixed.sameCustomer(127L, 127L)).isTrue();
        assertThat(fixed.sameCustomer(128L, 128L)).isTrue();
        assertThat(fixed.sameCustomer(100_000L, 100_000L)).isTrue();
        assertThat(fixed.sameCustomer(null, null)).isTrue();
        assertThat(fixed.sameCustomer(1L, null)).isFalse();
    }
}
