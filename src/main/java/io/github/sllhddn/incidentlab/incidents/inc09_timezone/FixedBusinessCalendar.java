package io.github.sllhddn.incidentlab.incidents.inc09_timezone;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * INC-09 (fixed). The business zone is explicit and the clock is injected, so the
 * result is the same on a laptop, in a UTC container and in a test.
 */
public class FixedBusinessCalendar implements BusinessCalendar {

    public static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");

    private final Clock clock;
    private final ZoneId businessZone;

    public FixedBusinessCalendar(Clock clock, ZoneId businessZone) {
        this.clock = clock;
        this.businessZone = businessZone;
    }

    @Override
    public LocalDate businessDateOf(Instant transactionTime) {
        return LocalDate.ofInstant(transactionTime, businessZone);
    }

    @Override
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), businessZone);
    }
}
