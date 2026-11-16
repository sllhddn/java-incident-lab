package io.github.sllhddn.incidentlab.incidents.inc10_cme;

import java.time.Instant;
import java.util.List;

/**
 * INC-10 (fixed). {@code removeIf} removes through the collection itself. For lists
 * shared between threads, the registry also needs a concurrent collection or a lock;
 * see the write-up.
 */
public class FixedSessionReaper implements SessionReaper {

    @Override
    public int removeExpired(List<Session> sessions, Instant now) {
        int before = sessions.size();
        sessions.removeIf(s -> s.isExpired(now));
        return before - sessions.size();
    }
}
