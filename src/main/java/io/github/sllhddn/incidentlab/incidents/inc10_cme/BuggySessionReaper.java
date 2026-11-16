package io.github.sllhddn.incidentlab.incidents.inc10_cme;

import java.time.Instant;
import java.util.List;

/**
 * INC-10 (as shipped). Removes from the list while iterating it with for-each.
 * The iterator's modCount check throws ConcurrentModificationException on the next
 * step, the scheduled job dies, and expired sessions pile up until restart.
 * It only "works" when the expired session happens to be second-to-last.
 */
public class BuggySessionReaper implements SessionReaper {

    @Override
    public int removeExpired(List<Session> sessions, Instant now) {
        int removed = 0;
        for (Session s : sessions) {
            if (s.isExpired(now)) {
                sessions.remove(s); // BUG: structural change during iteration
                removed++;
            }
        }
        return removed;
    }
}
