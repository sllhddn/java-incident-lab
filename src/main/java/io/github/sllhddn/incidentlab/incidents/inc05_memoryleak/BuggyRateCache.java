package io.github.sllhddn.incidentlab.incidents.inc05_memoryleak;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * INC-05 (as shipped). A static cache in front of a slow rate service.
 * The key includes the full timestamp, so almost every request is a miss and adds
 * a new entry. Nothing is ever evicted: old-gen grows until the JVM spends its time
 * in GC and finally throws OutOfMemoryError: Java heap space.
 */
public class BuggyRateCache implements RateLookup {

    private static final Map<String, BigDecimal> CACHE = new HashMap<>(); // BUG: static and unbounded

    private final Function<String, BigDecimal> rateService;

    public BuggyRateCache(Function<String, BigDecimal> rateService) {
        this.rateService = rateService;
    }

    @Override
    public BigDecimal rate(String currency, Instant at) {
        String key = currency + "@" + at; // BUG: key is unique per millisecond
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, k -> rateService.apply(currency));
        }
    }

    @Override
    public int cachedEntries() {
        synchronized (CACHE) {
            return CACHE.size();
        }
    }

    /** Test hook only: the static map outlives every instance, which is the point of the incident. */
    public static void clearForTests() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
