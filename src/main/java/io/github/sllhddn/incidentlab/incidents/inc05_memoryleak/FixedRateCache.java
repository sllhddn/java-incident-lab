package io.github.sllhddn.incidentlab.incidents.inc05_memoryleak;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * INC-05 (fixed). Rates are published once per business day, so the key is
 * currency + business date, and the map is an LRU bounded by {@code maxEntries}.
 * Instance-scoped, so its lifetime is the bean's, not the class loader's.
 */
public class FixedRateCache implements RateLookup {

    private final Function<String, BigDecimal> rateService;
    private final ZoneId zone;
    private final Map<String, BigDecimal> cache;

    public FixedRateCache(Function<String, BigDecimal> rateService, ZoneId zone, int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be >= 1");
        }
        this.rateService = rateService;
        this.zone = zone;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BigDecimal> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @Override
    public BigDecimal rate(String currency, Instant at) {
        String key = currency + "@" + LocalDate.ofInstant(at, zone);
        synchronized (cache) {
            return cache.computeIfAbsent(key, k -> rateService.apply(currency));
        }
    }

    @Override
    public int cachedEntries() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
