package io.github.sllhddn.incidentlab.incidents.inc07_money;

import java.math.BigDecimal;
import java.util.List;

/** Splits a total into equal monthly instalments. */
public interface InstalmentPlanner {

    List<BigDecimal> split(BigDecimal total, int months);
}
