package io.github.sllhddn.incidentlab.incidents.inc10_cme;

import java.time.Instant;

/** A user session that expires at a fixed time. */
public record Session(String id, Instant expiresAt) {

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
