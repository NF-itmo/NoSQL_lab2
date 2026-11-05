package org.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Pipeline;
import com.google.cloud.firestore.PipelineResult;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.pipeline.stages.Aggregate;
import lombok.extern.slf4j.Slf4j;
import org.models.OrderState;
import org.repository.DTO.BookStatusAnalytics;
import org.repository.DTO.OrderStatusAnalytics;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.google.cloud.firestore.pipeline.expressions.Expression.field;

@Repository
@Slf4j
public class OrderStatusFirestoreRepository {
    private static final String STATUS_LOG_COLLECTION = "OrderStatusLog";

    private final Firestore firestore;

    public OrderStatusFirestoreRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public void createLog(
            Transaction transaction,
            DocumentReference orderDocument,
            Integer bookId,
            String bookTitle,
            OrderState status,
            Instant timestamp
    ) {
        transaction.create(
                orderDocument.collection(STATUS_LOG_COLLECTION).document(UUID.randomUUID().toString()),
                Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "status", status.name(),
                        "timestamp", Date.from(timestamp)
                )
        );
    }

    public OrderStatusAnalytics getAnalytics(
            Instant from,
            Instant to,
            OrderState orderBy,
            int start,
            int limit
    ) {
        try {
            final Pipeline pipeline = firestore.pipeline()
                    .collectionGroup(STATUS_LOG_COLLECTION)
                    .where(field("timestamp").greaterThanOrEqual(Date.from(from)))
                    .where(field("timestamp").lessThanOrEqual(Date.from(to)))
                    .aggregate(Aggregate.withAccumulators(
                            field("status").equal(OrderState.PENDING.name()).countIf().as("pendingCount"),
                            field("status").equal(OrderState.IN_PROGRESS.name()).countIf().as("inProgressCount"),
                            field("status").equal(OrderState.DONE.name()).countIf().as("doneCount"),
                            field("status").equal(OrderState.ABORTED.name()).countIf().as("abortedCount")
                    ).withGroups(field("bookId"), field("bookTitle")))
                    .sort(field(countField(orderBy)).descending(), field("bookId").ascending())
                    .offset(start)
                    .limit(limit);

            final List<BookStatusAnalytics> books = pipeline.execute().get().getResults().stream()
                    .map(this::mapBookAnalytics)
                    .toList();

            return OrderStatusAnalytics.builder()
                    .books(books)
                    .build();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving order status analytics", exception);
            throw new RuntimeException(exception);
        }
    }

    private BookStatusAnalytics mapBookAnalytics(PipelineResult result) {
        return BookStatusAnalytics.builder()
                .bookId(Objects.requireNonNull(result.getLong("bookId")).intValue())
                .bookTitle(result.getString("bookTitle"))
                .pendingCount(count(result, "pendingCount"))
                .inProgressCount(count(result, "inProgressCount"))
                .doneCount(count(result, "doneCount"))
                .abortedCount(count(result, "abortedCount"))
                .build();
    }

    private long count(PipelineResult result, String field) {
        final Long count = result.getLong(field);
        return count == null ? 0 : count;
    }

    private String countField(OrderState state) {
        return switch (state) {
            case PENDING -> "pendingCount";
            case IN_PROGRESS -> "inProgressCount";
            case DONE -> "doneCount";
            case ABORTED -> "abortedCount";
        };
    }
}
