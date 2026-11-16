package io.github.sllhddn.incidentlab.incidents.inc02_nplusone;

import io.github.sllhddn.incidentlab.repo.OrderRepository;
import io.github.sllhddn.incidentlab.service.OrderSummary;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INC-02. The order list page went from 80 ms to 9 s as data grew, and the DB team
 * reported thousands of near-identical SELECTs per minute.
 */
@Service
public class OrderListingService {

    private final OrderRepository orders;

    public OrderListingService(OrderRepository orders) {
        this.orders = orders;
    }

    /**
     * As shipped: findAll() loads orders only. Building each summary touches the lazy
     * customer, the lazy lines collection and each line's lazy product, so N orders
     * cost 1 + N + N + (lines) queries.
     */
    @Transactional(readOnly = true)
    public List<OrderSummary> listBuggy() {
        return orders.findAll().stream().map(OrderSummary::of).toList(); // BUG: N+1
    }

    /** Fixed: one query with an entity graph fetches everything the summary needs. */
    @Transactional(readOnly = true)
    public List<OrderSummary> listFixed() {
        return orders.findAllWithDetails().stream().map(OrderSummary::of).toList();
    }
}
