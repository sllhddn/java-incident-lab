package io.github.sllhddn.incidentlab.incidents.inc06_deadlock;

import java.math.BigDecimal;

/** Moves money between two in-memory accounts atomically. */
public interface TransferService {

    void transfer(Account from, Account to, BigDecimal amount);

    /**
     * Test hook that runs after the first lock is taken. Production passes a no-op;
     * tests use it to force the unlucky interleaving every time instead of once a week.
     */
    void setAfterFirstLock(Runnable hook);
}
