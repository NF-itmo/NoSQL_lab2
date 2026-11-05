package org.services;

import lombok.extern.slf4j.Slf4j;
import org.exceptions.ConflictException;
import org.exceptions.NotFoundException;
import org.models.Order;
import org.models.OrderState;
import org.repository.BooksSearchCacheRepository;
import org.repository.DTO.OrderStatusAnalytics;
import org.repository.OrderStatusFirestoreRepository;
import org.repository.OrdersFirestoreRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class OrdersService {
    private final OrdersFirestoreRepository ordersRepository;
    private final BooksSearchCacheRepository booksSearchCacheRepository;
    private final OrderStatusFirestoreRepository orderStatusRepository;

    public OrdersService(
            OrdersFirestoreRepository ordersRepository,
            BooksSearchCacheRepository booksSearchCacheRepository,
            OrderStatusFirestoreRepository orderStatusRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.booksSearchCacheRepository = booksSearchCacheRepository;
        this.orderStatusRepository = orderStatusRepository;
    }

    public Order getById(Integer orderId) {
        return ordersRepository.getById(orderId).orElseThrow(
                () -> {
                    log.info("Order not found {}", orderId);
                    return new NotFoundException("Order not found");
                }
        );
    }

    public List<Order> getAll() {
        return ordersRepository.getAll();
    }

    public List<Order> search(
            OrderState status,
            Instant createdAfter,
            Instant createdBefore,
            Integer start,
            Integer limit
    ) {
        return ordersRepository.search(
                status,
                createdAfter,
                createdBefore,
                start == null ? 0 : start,
                limit == null ? 20 : limit
        );
    }

    public OrderStatusAnalytics getStatusAnalytics(
            Instant from,
            Instant to,
            OrderState orderBy,
            Integer start,
            Integer limit
    ) {
        return orderStatusRepository.getAnalytics(
                from,
                to,
                orderBy,
                start == null ? 0 : start,
                limit == null ? 20 : limit
        );
    }

    public void cancel(Integer orderId) {
        if (!ordersRepository.abort(orderId)) {
            throw new ConflictException("Order cannot be aborted");
        }
    }

    public void confirm(Integer orderId) {
        if (!ordersRepository.confirm(orderId)) {
            throw new ConflictException("Order cannot be started");
        }
    }

    public void complete(Integer orderId) {
        if (!ordersRepository.complete(orderId)) {
            throw new ConflictException("Order cannot be completed");
        }

        booksSearchCacheRepository.invalidate();
    }

    public Order create(Integer bookId) {
        return ordersRepository.createOrderByBookId(bookId);
    }
}
