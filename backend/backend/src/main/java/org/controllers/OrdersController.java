package org.controllers;

import jakarta.validation.Valid;
import org.controllers.DTO.CreateOrderRequest;
import org.controllers.DTO.GetBookStatusAnalyticsResponse;
import org.controllers.DTO.GetOrderResponse;
import org.controllers.DTO.GetOrderStatusAnalyticsResponse;
import org.models.Order;
import org.models.OrderState;
import org.repository.DTO.OrderStatusAnalytics;
import org.services.OrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/orders")
@PreAuthorize("isAuthenticated()")
public class OrdersController {
    private final OrdersService ordersService;

    public OrdersController(
            OrdersService ordersService
    ) {
        this.ordersService = ordersService;
    }

    @GetMapping
    public List<GetOrderResponse> getOrders() {
        return ordersService.getAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/search")
    public List<GetOrderResponse> searchOrders(
            @RequestParam(required = false) OrderState status,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer limit
    ) {
        return ordersService.search(status, createdAfter, createdBefore, start, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/analytics")
    public GetOrderStatusAnalyticsResponse getStatusAnalytics(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "PENDING") OrderState orderBy,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer limit
    ) {
        final OrderStatusAnalytics analytics = ordersService.getStatusAnalytics(
                from,
                to,
                orderBy,
                start,
                limit
        );

        return new GetOrderStatusAnalyticsResponse(
                analytics.getBooks().stream()
                        .map(book -> new GetBookStatusAnalyticsResponse(
                                book.getBookId(),
                                book.getBookTitle(),
                                book.getPendingCount(),
                                book.getInProgressCount(),
                                book.getDoneCount(),
                                book.getAbortedCount()
                        ))
                        .toList()
        );
    }

    @PostMapping
    public ResponseEntity<GetOrderResponse> createOrder(
            @RequestBody @Valid final CreateOrderRequest request
    ) {
        final Order order = ordersService.create(request.getBookId());

        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/{orderId}")
    public GetOrderResponse getOrder(
            @PathVariable Integer orderId
    ) {
        final Order order = ordersService.getById(orderId);

        return toResponse(order);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(
            @PathVariable Integer orderId
    ) {
        ordersService.confirm(orderId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<Void> completeOrder(
            @PathVariable Integer orderId
    ) {
        ordersService.complete(orderId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId
    ) {
        ordersService.cancel(Integer.parseInt(orderId));

        return ResponseEntity.noContent().build();
    }

    private GetOrderResponse toResponse(Order order) {
        return new GetOrderResponse(
                order.getId(),
                order.getBookId(),
                order.getBookLocation(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getStatus().getValue().name()
        );
    }
}
