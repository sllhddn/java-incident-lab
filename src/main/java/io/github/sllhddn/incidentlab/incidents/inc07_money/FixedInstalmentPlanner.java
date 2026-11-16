package io.github.sllhddn.incidentlab.incidents.inc07_money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * INC-07 (fixed). Works in whole sen with BigDecimal, rounds down, and gives the
 * leftover sen to the first instalments, so the parts always add up to the total.
 */
public class FixedInstalmentPlanner implements InstalmentPlanner {

    @Override
    public List<BigDecimal> split(BigDecimal total, int months) {
        if (months < 1) {
            throw new IllegalArgumentException("months must be >= 1");
        }
        if (total.scale() > 2) {
            throw new IllegalArgumentException("total has more than 2 decimal places: " + total);
        }
        BigDecimal sen = total.movePointRight(2);
        BigDecimal[] qr = sen.divideAndRemainder(BigDecimal.valueOf(months));
        BigDecimal base = qr[0];
        int remainder = qr[1].intValueExact();

        List<BigDecimal> out = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            BigDecimal part = i < remainder ? base.add(BigDecimal.ONE) : base;
            out.add(part.movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY));
        }
        return out;
    }
}
