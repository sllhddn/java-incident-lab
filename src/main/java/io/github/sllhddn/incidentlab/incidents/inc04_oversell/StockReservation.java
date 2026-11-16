package io.github.sllhddn.incidentlab.incidents.inc04_oversell;

/** Reserves stock for a purchase. */
public interface StockReservation {

    /** @return true if the units were reserved */
    boolean reserve(Long productId, int quantity);

    /**
     * Test hook that runs between "read" and "write". Production uses a no-op; tests put a
     * barrier here so every thread is inside the window at once, every run.
     */
    void setRaceWindow(Runnable hook);
}
