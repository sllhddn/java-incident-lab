package io.github.sllhddn.incidentlab.incidents.inc04_oversell;

import io.github.sllhddn.incidentlab.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INC-04 (fixed). The check and the decrement are one UPDATE with "stock >= qty" in
 * the WHERE clause. The database serialises writers on the row lock and re-checks the
 * condition, so the number of successful reservations can never exceed the stock.
 * (Optimistic locking with @Version plus a retry is the other valid fix; see the write-up.)
 */
@Service
public class AtomicStockReservation implements StockReservation {

    private final ProductRepository products;
    private volatile Runnable raceWindow = () -> { };

    public AtomicStockReservation(ProductRepository products) {
        this.products = products;
    }

    @Override
    @Transactional
    public boolean reserve(Long productId, int quantity) {
        raceWindow.run();
        return products.decrementStock(productId, quantity) == 1;
    }

    @Override
    public void setRaceWindow(Runnable hook) {
        this.raceWindow = hook;
    }
}
