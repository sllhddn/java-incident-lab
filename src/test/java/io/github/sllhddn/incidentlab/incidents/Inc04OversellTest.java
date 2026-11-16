package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.PostgresIntegrationTest;
import io.github.sllhddn.incidentlab.domain.Product;
import io.github.sllhddn.incidentlab.incidents.inc04_oversell.AtomicStockReservation;
import io.github.sllhddn.incidentlab.incidents.inc04_oversell.ReadThenWriteStockReservation;
import io.github.sllhddn.incidentlab.incidents.inc04_oversell.StockReservation;
import io.github.sllhddn.incidentlab.repo.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Inc04OversellTest extends PostgresIntegrationTest {

    static final int STOCK = 3;
    static final int BUYERS = 8; // below the Hikari pool size of 10

    @Autowired ReadThenWriteStockReservation readThenWrite;
    @Autowired AtomicStockReservation atomic;
    @Autowired ProductRepository products;

    @AfterEach
    void resetHooks() {
        readThenWrite.setRaceWindow(() -> { });
        atomic.setRaceWindow(() -> { });
    }

    @Test
    void reproduce_everyBuyerWinsTheLastUnits() throws Exception {
        Long productId = newProduct();

        int sold = flashSale(readThenWrite, productId);

        assertThat(sold).isEqualTo(BUYERS).isGreaterThan(STOCK); // oversold
        assertThat(products.currentStock(productId)).isEqualTo(STOCK - 1); // and stock looks healthy
    }

    @Test
    void fixed_neverSellsMoreThanTheStock() throws Exception {
        Long productId = newProduct();

        int sold = flashSale(atomic, productId);

        assertThat(sold).isEqualTo(STOCK);
        assertThat(products.currentStock(productId)).isZero();
    }

    private Long newProduct() {
        String sku = "FS-" + UUID.randomUUID().toString().substring(0, 8);
        return products.save(new Product(sku, "Flash sale item", new BigDecimal("1.00"), STOCK)).getId();
    }

    /** Every buyer reaches the race window before any of them continues. */
    private int flashSale(StockReservation reservation, Long productId) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(BUYERS);
        reservation.setRaceWindow(() -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < BUYERS; i++) {
                results.add(pool.submit(() -> reservation.reserve(productId, 1)));
            }
            int sold = 0;
            for (Future<Boolean> f : results) {
                if (f.get(30, TimeUnit.SECONDS)) {
                    sold++;
                }
            }
            return sold;
        } finally {
            pool.shutdownNow();
        }
    }
}
