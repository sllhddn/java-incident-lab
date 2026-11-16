package io.github.sllhddn.incidentlab.service;

import io.github.sllhddn.incidentlab.domain.CustomerOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** What the order list endpoint returns. Built inside the transaction, never lazily. */
public record OrderSummary(
        Long id,
        String customerName,
        LocalDate businessDate,
        BigDecimal total,
        List<Line> lines) {

    public record Line(String sku, int quantity, BigDecimal unitPrice) {
    }

    public static OrderSummary of(CustomerOrder o) {
        return new OrderSummary(
                o.getId(),
                o.getCustomer().getName(),
                o.getBusinessDate(),
                o.total(),
                o.getLines().stream()
                        .map(l -> new Line(l.getProduct().getSku(), l.getQuantity(), l.getUnitPrice()))
                        .toList());
    }
}
