package io.github.sllhddn.incidentlab.incidents.inc05_memoryleak;

import java.math.BigDecimal;
import java.time.Instant;

/** Looks up the exchange rate for a currency at a point in time. */
public interface RateLookup {

    BigDecimal rate(String currency, Instant at);

    /** Number of entries currently held in memory. */
    int cachedEntries();
}
