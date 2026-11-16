package io.github.sllhddn.incidentlab.service;

import io.github.sllhddn.incidentlab.domain.Customer;
import io.github.sllhddn.incidentlab.domain.CustomerOrder;
import io.github.sllhddn.incidentlab.domain.Product;
import io.github.sllhddn.incidentlab.incidents.inc01_npe.DiscountPolicy;
import io.github.sllhddn.incidentlab.incidents.inc09_timezone.BusinessCalendar;
import io.github.sllhddn.incidentlab.repo.CustomerRepository;
import io.github.sllhddn.incidentlab.repo.OrderRepository;
import io.github.sllhddn.incidentlab.repo.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The production path. It uses the fixed version of every incident that touches it:
 * atomic stock reservation (INC-04), one-query order listing (INC-02),
 * tier discounts that tolerate unknown tiers (INC-01) and an explicit business zone (INC-09).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final DiscountPolicy discountPolicy;
    private final BusinessCalendar calendar;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, CustomerRepository customers,
                        DiscountPolicy discountPolicy, BusinessCalendar calendar, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.customers = customers;
        this.discountPolicy = discountPolicy;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Transactional
    public OrderSummary placeOrder(Long customerId, String sku, int quantity) {
        Long productId = products.findBySku(sku)
                .map(Product::getId)
                .orElseThrow(() -> new NotFoundException("Unknown SKU " + sku));

        // Reserve first, in one statement. The repository clears the persistence context
        // afterwards, so entities are loaded only after this line.
        if (products.decrementStock(productId, quantity) == 0) {
            throw new OutOfStockException(sku, quantity);
        }

        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Unknown customer " + customerId));
        Product product = products.findById(productId).orElseThrow();

        Instant now = clock.instant();
        CustomerOrder order = new CustomerOrder(customer, calendar.businessDateOf(now), now);
        order.addLine(product, quantity);
        order.applyDiscount(discountPolicy.discountFor(customer.getTier(), order.getSubtotal()));
        orders.save(order);

        log.info("order placed id={} customer={} sku={} qty={} total={}",
                order.getId(), customerId, sku, quantity, order.total());
        return OrderSummary.of(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listOrders() {
        return orders.findAllWithDetails().stream().map(OrderSummary::of).toList();
    }
}
