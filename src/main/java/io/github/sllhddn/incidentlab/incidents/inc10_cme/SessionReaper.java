package io.github.sllhddn.incidentlab.incidents.inc10_cme;

import java.time.Instant;
import java.util.List;

/** Removes expired sessions from the in-memory registry. */
public interface SessionReaper {

    /** @return how many sessions were removed */
    int removeExpired(List<Session> sessions, Instant now);
}
