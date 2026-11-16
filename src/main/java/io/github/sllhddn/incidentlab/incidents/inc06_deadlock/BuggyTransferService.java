package io.github.sllhddn.incidentlab.incidents.inc06_deadlock;

import java.math.BigDecimal;

/**
 * INC-06 (as shipped). Locks "from" then "to". When A pays B while B pays A,
 * thread 1 holds A and waits for B, thread 2 holds B and waits for A: a classic
 * lock-ordering deadlock. CPU is idle, requests hang, and {@code jstack} prints
 * "Found one Java-level deadlock".
 */
public class BuggyTransferService implements TransferService {

    private volatile Runnable afterFirstLock = () -> { };

    @Override
    public void transfer(Account from, Account to, BigDecimal amount) {
        synchronized (from) {          // BUG: lock order depends on the call direction
            afterFirstLock.run();
            synchronized (to) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }

    @Override
    public void setAfterFirstLock(Runnable hook) {
        this.afterFirstLock = hook;
    }
}
