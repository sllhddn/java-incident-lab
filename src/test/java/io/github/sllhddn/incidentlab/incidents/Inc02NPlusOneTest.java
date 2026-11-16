package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.PostgresIntegrationTest;
import io.github.sllhddn.incidentlab.domain.Product;
import io.github.sllhddn.incidentlab.incidents.inc02_nplusone.OrderListingService;
import io.github.sllhddn.incidentlab.repo.ProductRepository;
import io.github.sllhddn.incidentlab.service.OrderService;
import io.github.sllhddn.incidentlab.service.OrderSummary;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Inc02NPlusOneTest extends PostgresIntegrationTest {

    @Autowired OrderListingService listing;
    @Autowired OrderService orderService;
    @Autowired ProductRepository products;
    @Autowired EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void seedOrders() {
        String sku = "N1-" + UUID.randomUUID().toString().substring(0, 8);
        products.save(new Product(sku, "N+1 test item", new BigDecimal("10.00"), 1_000));
        for (int i = 0; i < 20; i++) {
            orderService.placeOrder((long) (i % 3) + 1, sku, 1);
        }
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    @Test
    void reproduce_onePlusNQueries() {
        List<OrderSummary> result = listing.listBuggy();

        long statements = stats.getPrepareStatementCount();
        assertThat(result).hasSizeGreaterThanOrEqualTo(20);
        // One for the orders, then at least one per order for its lines.
        assertThat(statements).isGreaterThanOrEqualTo(1L + result.size());
    }

    @Test
    void fixed_singleQuery() {
        List<OrderSummary> result = listing.listFixed();

        assertThat(result).hasSizeGreaterThanOrEqualTo(20);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    void bothVersionsReturnTheSameData() {
        assertThat(listing.listFixed()).containsExactlyInAnyOrderElementsOf(listing.listBuggy());
    }
}
