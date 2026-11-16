package io.github.sllhddn.incidentlab.incidents.inc09_timezone;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * INC-09 (as shipped). Uses the JVM default time zone. Developers' laptops are on
 * Asia/Kuala_Lumpur, so it worked locally; the container runs in UTC, so every
 * transaction between 00:00 and 07:59 Malaysia time was booked to the previous day
 * and end-of-day settlement totals did not match the bank's file.
 */
public class BuggyBusinessCalendar implements BusinessCalendar {

    private final ZoneId jvmDefault;

    /** Production called {@code ZoneId.systemDefault()}; the constructor makes that visible. */
    public BuggyBusinessCalendar(ZoneId jvmDefault) {
        this.jvmDefault = jvmDefault;
    }

    public BuggyBusinessCalendar() {
        this(ZoneId.systemDefault());
    }

    @Override
    public LocalDate businessDateOf(Instant transactionTime) {
        return LocalDate.ofInstant(transactionTime, jvmDefault); // BUG: depends on where it runs
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(); // BUG: hidden clock + hidden zone, impossible to test
    }
}
