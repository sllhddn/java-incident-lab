package io.github.sllhddn.incidentlab.incidents.inc06_deadlock;

import java.math.BigDecimal;

/**
 * INC-06 (fixed). Always lock the account with the lower id first, whatever the
 * direction of the transfer. With one global order a cycle of waiters cannot form.
 */
public class FixedTransferService implements TransferService {

    private volatile Runnable afterFirstLock = () -> { };

    @Override
    public void transfer(Account from, Account to, BigDecimal amount) {
        if (from.id() == to.id()) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        Account first = from.id() < to.id() ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) {
            afterFirstLock.run();
            synchronized (second) {
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
