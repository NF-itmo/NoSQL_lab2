package org.services;

import lombok.extern.slf4j.Slf4j;
import org.exceptions.ConflictException;
import org.exceptions.NotFoundException;
import org.models.Order;
import org.repository.OrdersFirestoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OrdersService {
    private final OrdersFirestoreRepository ordersRepository;

    public OrdersService(
            OrdersFirestoreRepository ordersRepository
    ) {
        this.ordersRepository = ordersRepository;
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

    public void cancel(Integer orderId) {
        ordersRepository.deleteById(orderId);
    }

    public void confirm(Integer orderId) {
        final Order order = getById(orderId);
        final boolean confirmed = ordersRepository.confirm(orderId, order.getBookId());

        if (!confirmed) {
            throw new ConflictException("Book is unavailable or order has expired");
        }
    }

    public Order create(Integer bookId) {
        return ordersRepository.createOrderByBookId(bookId);
    }
}
