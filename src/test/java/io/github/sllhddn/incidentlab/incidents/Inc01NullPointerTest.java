package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sllhddn.incidentlab.incidents.inc01_npe.BuggyDiscountPolicy;
import io.github.sllhddn.incidentlab.incidents.inc01_npe.FixedDiscountPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class Inc01NullPointerTest {

    @Test
    void reproduce_unknownTierUnboxesNull() {
        var buggy = new BuggyDiscountPolicy();

        assertThat(buggy.discountFor("GOLD", new BigDecimal("200.00"))).isEqualByComparingTo("20.00");
        assertThatThrownBy(() -> buggy.discountFor("PLATINUM", new BigDecimal("200.00")))
                .isInstanceOf(NullPointerException.class)
                // Helpful NPE messages (JDK 14+) name the exact expression that was null.
                .hasMessageContaining("Map.get(Object)")
                .hasMessageContaining("intValue()");
    }

    @Test
    void fixed_knownAndUnknownTiers() {
        var fixed = new FixedDiscountPolicy();

        assertThat(fixed.discountFor("PLATINUM", new BigDecimal("200.00"))).isEqualByComparingTo("30.00");
        assertThat(fixed.discountFor("DIAMOND", new BigDecimal("200.00"))).isEqualByComparingTo("0.00");
        assertThat(fixed.discountFor(null, new BigDecimal("200.00"))).isEqualByComparingTo("0.00");
    }
}
