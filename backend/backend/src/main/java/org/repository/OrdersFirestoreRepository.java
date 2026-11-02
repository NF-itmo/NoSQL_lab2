package org.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.models.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class OrdersFirestoreRepository {
    private final Firestore firestore;
    private final String ordersCollectionName;
    private final String booksCollectionName;
    private final long orderTtlSeconds;

    public OrdersFirestoreRepository(
            Firestore firestore,
            @Value("orders") String ordersCollectionName,
            @Value("books") String booksCollectionName,
            @Value("${orders-service.order-ttl-seconds}") long orderTtlSeconds
    ) {
        this.firestore = firestore;
        this.ordersCollectionName = ordersCollectionName;
        this.booksCollectionName = booksCollectionName;
        this.orderTtlSeconds = orderTtlSeconds;
    }

    public Optional<Order> getById(Integer orderId) {
        try {
            final DocumentSnapshot snapshot = orderDocument(orderId)
                    .get()
                    .get();

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            final Order order = mapOrder(snapshot);
            return order.getExpiresAt().isAfter(Instant.now()) ? Optional.of(order) : Optional.empty();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving order {}", orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    public List<Order> getAll() {
        try {
            return firestore.collection(ordersCollectionName)
                    .whereGreaterThan("expiresAt", Timestamp.now())
                    .orderBy("expiresAt")
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::mapOrder)
                    .toList();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving orders", exception);
            throw new RuntimeException(exception);
        }
    }

    public void deleteById(Integer orderId) {
        try {
            orderDocument(orderId).delete().get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while deleting order {}", orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    public Order createOrderByBookId(Integer bookId) {
        final Order order = Order.builder()
                .id(bookId)
                .bookId(bookId)
                .expiresAt(Instant.now().plusSeconds(orderTtlSeconds))
                .build();

        try {
            orderDocument(order.getId()).set(toDocument(order)).get();
            return order;
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while creating order for book {}", bookId, exception);
            throw new RuntimeException(exception);
        }
    }

    public boolean confirm(Integer orderId, Integer bookId) {
        try {
            return firestore.runTransaction(transaction -> confirmOrder(
                            transaction,
                            orderId,
                            bookId
                    ))
                    .get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while confirming order {}", orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    private boolean confirmOrder(
            Transaction transaction,
            Integer orderId,
            Integer bookId
    ) {
        try {
            final DocumentReference orderDocument = orderDocument(orderId);
            final DocumentReference bookDocument = firestore.collection(booksCollectionName)
                    .document(String.valueOf(bookId));

            final DocumentSnapshot orderSnapshot = transaction.get(orderDocument).get();
            final DocumentSnapshot bookSnapshot = transaction.get(bookDocument).get();

            if (!orderSnapshot.exists() || !bookSnapshot.exists()) {
                return false;
            }

            final Timestamp expiresAt = orderSnapshot.getTimestamp("expiresAt");
            final Boolean available = bookSnapshot.getBoolean("available");

            if (expiresAt == null || !expiresAt.toDate().toInstant().isAfter(Instant.now())
                    || !Boolean.TRUE.equals(available)) {
                return false;
            }

            transaction.update(bookDocument, "available", false);
            transaction.delete(orderDocument);
            return true;
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while confirming order {}", orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    private DocumentReference orderDocument(Integer orderId) {
        return firestore.collection(ordersCollectionName).document(String.valueOf(orderId));
    }

    private Map<String, Object> toDocument(Order order) {
        return Map.of(
                "bookId", order.getBookId(),
                "expiresAt", Timestamp.ofTimeSecondsAndNanos(
                        order.getExpiresAt().getEpochSecond(),
                        order.getExpiresAt().getNano()
                )
        );
    }

    private Order mapOrder(DocumentSnapshot snapshot) {
        final Timestamp expiresAt = snapshot.getTimestamp("expiresAt");

        return Order.builder()
                .id(Integer.parseInt(snapshot.getId()))
                .bookId(Objects.requireNonNull(snapshot.getLong("bookId")).intValue())
                .expiresAt(Objects.requireNonNull(expiresAt).toDate().toInstant())
                .build();
    }
}
