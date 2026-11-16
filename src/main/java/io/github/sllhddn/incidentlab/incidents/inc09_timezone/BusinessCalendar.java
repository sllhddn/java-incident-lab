package io.github.sllhddn.incidentlab.incidents.inc09_timezone;

import java.time.Instant;
import java.time.LocalDate;

/** Maps a transaction time to the business date it settles on. */
public interface BusinessCalendar {

    LocalDate businessDateOf(Instant transactionTime);

    LocalDate today();
}
