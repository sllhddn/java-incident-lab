package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.incidents.inc05_memoryleak.BuggyRateCache;
import io.github.sllhddn.incidentlab.incidents.inc05_memoryleak.FixedRateCache;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Inc05MemoryLeakTest {

    static final Instant START = Instant.parse("2026-11-16T01:00:00Z");

    @AfterEach
    void clear() {
        BuggyRateCache.clearForTests();
    }

    @Test
    void reproduce_oneEntryPerRequestForever() {
        AtomicInteger calls = new AtomicInteger();
        var cache = new BuggyRateCache(c -> { calls.incrementAndGet(); return new BigDecimal("4.20"); });

        for (int i = 0; i < 10_000; i++) {
            cache.rate("USD", START.plusMillis(i));
        }

        assertThat(cache.cachedEntries()).isEqualTo(10_000); // grows with traffic, never shrinks
        assertThat(calls).hasValue(10_000);                   // and it never even hits
        // A second instance shares the same static map: the leak outlives the bean.
        assertThat(new BuggyRateCache(c -> BigDecimal.ONE).cachedEntries()).isEqualTo(10_000);
    }

    @Test
    void fixed_keyedByBusinessDayAndBounded() {
        AtomicInteger calls = new AtomicInteger();
        var cache = new FixedRateCache(c -> { calls.incrementAndGet(); return new BigDecimal("4.20"); },
                ZoneId.of("Asia/Kuala_Lumpur"), 100);

        for (int i = 0; i < 10_000; i++) {
            cache.rate("USD", START.plusMillis(i));
        }
        assertThat(cache.cachedEntries()).isEqualTo(1);
        assertThat(calls).hasValue(1);

        for (int day = 0; day < 1_000; day++) {
            cache.rate("EUR", START.plus(Duration.ofDays(day)));
        }
        assertThat(cache.cachedEntries()).isEqualTo(100); // LRU cap holds
    }
}
