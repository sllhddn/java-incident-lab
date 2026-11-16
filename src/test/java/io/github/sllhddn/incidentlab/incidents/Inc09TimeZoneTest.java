package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.incidents.inc09_timezone.BuggyBusinessCalendar;
import io.github.sllhddn.incidentlab.incidents.inc09_timezone.FixedBusinessCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class Inc09TimeZoneTest {

    // 00:30 on 16 Nov in Kuala Lumpur is 16:30 on 15 Nov in UTC.
    static final Instant JUST_AFTER_MIDNIGHT_MY =
            ZonedDateTime.of(2026, 11, 16, 0, 30, 0, 0, FixedBusinessCalendar.MALAYSIA).toInstant();

    @Test
    void reproduce_containerInUtcBooksToYesterday() {
        var onLaptop = new BuggyBusinessCalendar(FixedBusinessCalendar.MALAYSIA);
        var inContainer = new BuggyBusinessCalendar(ZoneOffset.UTC);

        assertThat(onLaptop.businessDateOf(JUST_AFTER_MIDNIGHT_MY)).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(inContainer.businessDateOf(JUST_AFTER_MIDNIGHT_MY)).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void fixed_sameAnswerEverywhere() {
        var calendar = new FixedBusinessCalendar(
                Clock.fixed(JUST_AFTER_MIDNIGHT_MY, ZoneOffset.UTC), FixedBusinessCalendar.MALAYSIA);

        assertThat(calendar.businessDateOf(JUST_AFTER_MIDNIGHT_MY)).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(calendar.today()).isEqualTo(LocalDate.of(2026, 11, 16));
    }
}
