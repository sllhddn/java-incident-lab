package io.github.sllhddn.incidentlab.incidents.inc04_oversell;

import io.github.sllhddn.incidentlab.domain.Product;
import io.github.sllhddn.incidentlab.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INC-04 (as shipped). Read the stock, check it in Java, write back "stock - qty".
 * Under a flash sale, many requests read the same value before any of them writes,
 * all pass the check, and the shop sold 3x the units it had. {@code @Transactional}
 * does not help: at READ COMMITTED nothing stops two transactions reading the same row.
 */
@Service
public class ReadThenWriteStockReservation implements StockReservation {

    private final ProductRepository products;
    private volatile Runnable raceWindow = () -> { };

    public ReadThenWriteStockReservation(ProductRepository products) {
        this.products = products;
    }

    @Override
    @Transactional
    public boolean reserve(Long productId, int quantity) {
        Product product = products.findById(productId).orElseThrow();
        int current = product.getStock();
        raceWindow.run();
        if (current < quantity) {
            return false;
        }
        products.overwriteStock(productId, current - quantity); // BUG: lost update
        return true;
    }

    @Override
    public void setRaceWindow(Runnable hook) {
        this.raceWindow = hook;
    }
}
