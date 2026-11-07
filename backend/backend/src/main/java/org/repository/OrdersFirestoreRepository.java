package org.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Filter;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteBatch;
import lombok.extern.slf4j.Slf4j;
import org.models.Order;
import org.models.OrderState;
import org.models.OrderStatus;
import org.models.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
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
    private final OrderStatusFirestoreRepository orderStatusRepository;

    public OrdersFirestoreRepository(
            Firestore firestore,
            OrderStatusFirestoreRepository orderStatusRepository,
            @Value("orders") String ordersCollectionName,
            @Value("books") String booksCollectionName,
            @Value("${orders-service.order-ttl-seconds}") long orderTtlSeconds
    ) {
        this.firestore = firestore;
        this.orderStatusRepository = orderStatusRepository;
        this.ordersCollectionName = ordersCollectionName;
        this.booksCollectionName = booksCollectionName;
        this.orderTtlSeconds = orderTtlSeconds;
    }

    public Optional<Order> getById(Integer orderId) {
        try {
            final DocumentSnapshot snapshot = orderDocument(orderId).get().get();

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            final Order order = mapOrder(snapshot);
            if (order.getExpiresAt().isAfter(Instant.now())) {
                return Optional.of(order);
            }

            deleteById(orderId);
            return Optional.empty();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving order {}", orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    public List<Order> getAll() {
        deleteExpired();

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

    public List<Order> search(
            OrderState status,
            Instant createdAfter,
            Instant createdBefore,
            int start,
            int limit
    ) {
        deleteExpired();

        try {
            final List<Filter> filters = new ArrayList<>();
            if (status != null) {
                filters.add(Filter.or(
                        Filter.equalTo("status", status.name()),
                        Filter.equalTo("statusObject.value", status.name())
                ));
            }
            if (createdAfter != null) {
                filters.add(Filter.greaterThanOrEqualTo("createdAt", Date.from(createdAfter)));
            }
            if (createdBefore != null) {
                filters.add(Filter.lessThanOrEqualTo("createdAt", Date.from(createdBefore)));
            }

            Query query = firestore.collection(ordersCollectionName);
            if (!filters.isEmpty()) {
                query = query.where(Filter.and(filters.toArray(Filter[]::new)));
            }

            return query.orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(start)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::mapOrder)
                    .toList();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while searching orders", exception);
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
        final Instant now = Instant.now();
        final BookOrderDetails book = getBookDetails(bookId);
        final Order order = Order.builder()
                .id(bookId)
                .bookId(bookId)
                .bookTitle(book.title())
                .bookLocation(book.location())
                .createdAt(now)
                .expiresAt(now.plusSeconds(orderTtlSeconds))
                .status(OrderStatus.builder()
                        .value(OrderState.PENDING)
                        .changedAt(now)
                        .build())
                .build();

        try {
            return firestore.runTransaction(transaction -> {
                transaction.create(orderDocument(order.getId()), Map.of(
                        "bookId", order.getBookId(),
                        "bookTitle", order.getBookTitle(),
                        "bookLocation", Map.of(
                                "hall", order.getBookLocation().getHall(),
                                "rack", order.getBookLocation().getRack(),
                                "shelf", order.getBookLocation().getShelf()
                        ),
                        "createdAt", Date.from(order.getCreatedAt()),
                        "expiresAt", Date.from(order.getExpiresAt()),
                        "status", order.getStatus().getValue().name()
                ));
                orderStatusRepository.createLog(
                        transaction,
                        orderDocument(order.getId()),
                        order.getBookId(),
                        order.getBookTitle(),
                        OrderState.PENDING,
                        now
                );
                return order;
            }).get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while creating order for book {}", bookId, exception);
            throw new RuntimeException(exception);
        }
    }

    public boolean confirm(Integer orderId) {
        return runTransaction(orderId, "confirming", transaction -> {
                final DocumentReference orderDocument = orderDocument(orderId);
                final DocumentSnapshot orderSnapshot = transaction.get(orderDocument).get();
                final Timestamp expiresAt = orderSnapshot.getTimestamp("expiresAt");

                if (!orderSnapshot.exists() || getStatus(orderSnapshot) != OrderState.PENDING
                        || expiresAt == null || !expiresAt.toDate().toInstant().isAfter(Instant.now())) {
                    return false;
                }

                final Integer bookId = getBookId(orderSnapshot);
                final DocumentSnapshot bookSnapshot = transaction.get(bookDocument(bookId)).get();
                if (!bookSnapshot.exists() || !Boolean.TRUE.equals(bookSnapshot.getBoolean("available"))) {
                    return false;
                }

                setStatus(
                        transaction,
                        orderDocument,
                        orderSnapshot,
                        bookId,
                        getBookTitle(orderSnapshot),
                        OrderState.IN_PROGRESS
                );
                return true;
            });
    }

    public boolean complete(Integer orderId) {
        return runTransaction(orderId, "completing", transaction -> {
                final DocumentReference orderDocument = orderDocument(orderId);
                final DocumentSnapshot orderSnapshot = transaction.get(orderDocument).get();

                if (!orderSnapshot.exists() || getStatus(orderSnapshot) != OrderState.IN_PROGRESS) {
                    return false;
                }

                final Integer bookId = getBookId(orderSnapshot);
                final DocumentReference bookDocument = bookDocument(bookId);
                final DocumentSnapshot bookSnapshot = transaction.get(bookDocument).get();
                if (!bookSnapshot.exists() || !Boolean.TRUE.equals(bookSnapshot.getBoolean("available"))) {
                    return false;
                }

                transaction.update(bookDocument, "available", false);
                final Instant now = Instant.now();
                orderStatusRepository.createLog(
                        transaction,
                        orderDocument,
                        bookId,
                        getBookTitle(orderSnapshot),
                        OrderState.DONE,
                        now
                );
                transaction.delete(orderDocument);
                return true;
            });
    }

    public boolean abort(Integer orderId) {
        return runTransaction(orderId, "aborting", transaction -> {
                final DocumentReference orderDocument = orderDocument(orderId);
                final DocumentSnapshot orderSnapshot = transaction.get(orderDocument).get();

                if (!orderSnapshot.exists()) {
                    return false;
                }

                final OrderState currentState = getStatus(orderSnapshot);
                if (currentState != OrderState.PENDING && currentState != OrderState.IN_PROGRESS) {
                    return false;
                }

                orderStatusRepository.createLog(
                        transaction,
                        orderDocument,
                        getBookId(orderSnapshot),
                        getBookTitle(orderSnapshot),
                        OrderState.ABORTED,
                        Instant.now()
                );
                transaction.delete(orderDocument);
                return true;
            });
    }

    private void deleteExpired() {
        try {
            while (true) {
                final List<QueryDocumentSnapshot> expiredOrders = firestore.collection(ordersCollectionName)
                        .whereLessThanOrEqualTo("expiresAt", Timestamp.now())
                        .limit(500)
                        .get()
                        .get()
                        .getDocuments();

                if (expiredOrders.isEmpty()) {
                    return;
                }

                final WriteBatch batch = firestore.batch();
                expiredOrders.forEach(snapshot -> batch.delete(snapshot.getReference()));
                batch.commit().get();
            }
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while deleting expired orders", exception);
            throw new RuntimeException(exception);
        }
    }

    private boolean runTransaction(
            Integer orderId,
            String action,
            Transaction.Function<Boolean> operation
    ) {
        try {
            return firestore.runTransaction(operation).get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while {} order {}", action, orderId, exception);
            throw new RuntimeException(exception);
        }
    }

    private DocumentReference orderDocument(Integer orderId) {
        return firestore.collection(ordersCollectionName).document(String.valueOf(orderId));
    }

    private DocumentReference bookDocument(Integer bookId) {
        return firestore.collection(booksCollectionName).document(String.valueOf(bookId));
    }

    private Order mapOrder(DocumentSnapshot snapshot) {
        final Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
        final Timestamp createdAt = snapshot.getTimestamp("createdAt");
        final OrderState status = getStatus(snapshot);

        return Order.builder()
                .id(Integer.parseInt(snapshot.getId()))
                .bookId(getBookId(snapshot))
                .bookTitle(getBookTitle(snapshot))
                .bookLocation(getBookLocation(snapshot))
                .createdAt(Objects.requireNonNull(createdAt).toDate().toInstant())
                .expiresAt(Objects.requireNonNull(expiresAt).toDate().toInstant())
                .status(OrderStatus.builder()
                        .value(status)
                        .changedAt(getStatusChangedAt(snapshot, createdAt))
                        .build())
                .build();
    }

    private void setStatus(
            Transaction transaction,
            DocumentReference orderDocument,
            DocumentSnapshot orderSnapshot,
            Integer bookId,
            String bookTitle,
            OrderState status
    ) {
        final Instant now = Instant.now();
        if (orderSnapshot.contains("statusObject")) {
            transaction.update(orderDocument, Map.of(
                    "statusObject.value", status.name(),
                    "statusObject.changedAt", Date.from(now)
            ));
        } else {
            transaction.update(orderDocument, "status", status.name());
        }
        orderStatusRepository.createLog(
                transaction,
                orderDocument,
                bookId,
                bookTitle,
                status,
                now
        );
    }

    private Integer getBookId(DocumentSnapshot orderSnapshot) {
        return Objects.requireNonNull(orderSnapshot.getLong("bookId")).intValue();
    }

    private String getBookTitle(DocumentSnapshot orderSnapshot) {
        return Objects.requireNonNull(orderSnapshot.getString("bookTitle"));
    }

    private BookOrderDetails getBookDetails(Integer bookId) {
        try {
            final DocumentSnapshot book = bookDocument(bookId).get().get();
            if (!book.exists()) {
                throw new IllegalArgumentException("Book not found");
            }

            return new BookOrderDetails(
                    Objects.requireNonNull(book.getString("title")),
                    Location.builder()
                            .hall(Objects.requireNonNull(book.getLong("location.hall")).intValue())
                            .rack(Objects.requireNonNull(book.getLong("location.rack")).intValue())
                            .shelf(Objects.requireNonNull(book.getLong("location.shelf")).intValue())
                            .build()
            );
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving book {}", bookId, exception);
            throw new RuntimeException(exception);
        }
    }

    private OrderState getStatus(DocumentSnapshot snapshot) {
        final String statusObject = snapshot.getString("statusObject.value");
        if (statusObject != null) {
            return OrderState.valueOf(statusObject);
        }

        return OrderState.valueOf(Objects.requireNonNull(snapshot.getString("status")));
    }

    private Instant getStatusChangedAt(DocumentSnapshot snapshot, Timestamp createdAt) {
        final Timestamp changedAt = snapshot.getTimestamp("statusObject.changedAt");
        return (changedAt == null ? Objects.requireNonNull(createdAt) : changedAt)
                .toDate()
                .toInstant();
    }

    private Location getBookLocation(DocumentSnapshot snapshot) {
        if (!snapshot.contains("bookLocation")) {
            return null;
        }

        return Location.builder()
                .hall(Objects.requireNonNull(snapshot.getLong("bookLocation.hall")).intValue())
                .rack(Objects.requireNonNull(snapshot.getLong("bookLocation.rack")).intValue())
                .shelf(Objects.requireNonNull(snapshot.getLong("bookLocation.shelf")).intValue())
                .build();
    }

    private record BookOrderDetails(String title, Location location) {}
}
