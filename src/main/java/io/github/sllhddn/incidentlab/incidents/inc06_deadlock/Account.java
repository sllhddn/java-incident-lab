package io.github.sllhddn.incidentlab.incidents.inc06_deadlock;

import java.math.BigDecimal;

/** In-memory wallet account. Each instance is its own monitor. */
public final class Account {

    private final long id;
    private BigDecimal balance;

    public Account(long id, BigDecimal opening) {
        this.id = id;
        this.balance = opening;
    }

    public long id() {
        return id;
    }

    public synchronized BigDecimal balance() {
        return balance;
    }

    void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in account " + id);
        }
        balance = balance.subtract(amount);
    }

    void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
