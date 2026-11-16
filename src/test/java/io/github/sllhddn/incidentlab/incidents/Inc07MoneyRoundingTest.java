package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.incidents.inc07_money.BuggyInstalmentPlanner;
import io.github.sllhddn.incidentlab.incidents.inc07_money.FixedInstalmentPlanner;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class Inc07MoneyRoundingTest {

    static BigDecimal sum(List<BigDecimal> parts) {
        return parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void reproduce_aSenGoesMissing() {
        List<BigDecimal> plan = new BuggyInstalmentPlanner().split(new BigDecimal("100.00"), 3);

        assertThat(plan).containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(sum(plan)).isEqualByComparingTo("99.99");
        // The root cause in one line: 0.1 has no exact binary representation.
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
    }

    @Test
    void fixed_partsAlwaysAddUpToTheTotal() {
        var planner = new FixedInstalmentPlanner();

        assertThat(planner.split(new BigDecimal("100.00"), 3))
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));

        for (int sen = 1; sen <= 20_000; sen += 7) {
            BigDecimal total = BigDecimal.valueOf(sen, 2);
            for (int months = 1; months <= 24; months++) {
                List<BigDecimal> plan = planner.split(total, months);
                assertThat(sum(plan)).isEqualByComparingTo(total);
                assertThat(plan).allSatisfy(p -> assertThat(p.scale()).isEqualTo(2));
            }
        }
    }
}
