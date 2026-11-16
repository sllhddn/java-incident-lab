package io.github.sllhddn.incidentlab.incidents.inc07_money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * INC-07 (as shipped). Uses double arithmetic and rounds each instalment on its own.
 * RM 100.00 over 3 months becomes 33.33 + 33.33 + 33.33 = 99.99: one sen disappears
 * per plan, and finance's month-end reconciliation does not balance.
 */
public class BuggyInstalmentPlanner implements InstalmentPlanner {

    @Override
    public List<BigDecimal> split(BigDecimal total, int months) {
        double each = total.doubleValue() / months; // BUG: binary floating point for money
        List<BigDecimal> out = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            out.add(BigDecimal.valueOf(each).setScale(2, RoundingMode.HALF_UP)); // BUG: remainder lost
        }
        return out;
    }
}
