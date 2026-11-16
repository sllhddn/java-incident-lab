package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sllhddn.incidentlab.incidents.inc10_cme.BuggySessionReaper;
import io.github.sllhddn.incidentlab.incidents.inc10_cme.FixedSessionReaper;
import io.github.sllhddn.incidentlab.incidents.inc10_cme.Session;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class Inc10ConcurrentModificationTest {

    static final Instant NOW = Instant.parse("2026-11-16T02:00:00Z");

    static List<Session> sessions(boolean... expired) {
        List<Session> list = new ArrayList<>();
        for (int i = 0; i < expired.length; i++) {
            list.add(new Session("s" + i, expired[i] ? NOW.minusSeconds(60) : NOW.plusSeconds(600)));
        }
        return list;
    }

    @Test
    void reproduce_removeInsideForEach() {
        var buggy = new BuggySessionReaper();

        assertThatThrownBy(() -> buggy.removeExpired(sessions(true, false, true, false), NOW))
                .isInstanceOf(ConcurrentModificationException.class);

        // Why it survived code review and tests: removing the second-to-last element
        // ends the loop early (hasNext() is false), so no exception and no hint.
        List<Session> lucky = sessions(false, true, false);
        assertThat(buggy.removeExpired(lucky, NOW)).isEqualTo(1);
    }

    @Test
    void fixed_removeIf() {
        List<Session> list = sessions(true, false, true, false);

        assertThat(new FixedSessionReaper().removeExpired(list, NOW)).isEqualTo(2);
        assertThat(list).extracting(Session::id).containsExactly("s1", "s3");
    }
}
