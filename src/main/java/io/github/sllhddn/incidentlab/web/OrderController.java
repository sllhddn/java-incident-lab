package io.github.sllhddn.incidentlab.web;

import io.github.sllhddn.incidentlab.service.OrderService;
import io.github.sllhddn.incidentlab.service.OrderSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    public record PlaceOrderRequest(
            @NotNull Long customerId,
            @NotBlank String sku,
            @Min(1) @Max(100) int quantity) {
    }

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderSummary place(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request.customerId(), request.sku(), request.quantity());
    }

    @GetMapping
    public List<OrderSummary> list() {
        return orderService.listOrders();
    }
}
