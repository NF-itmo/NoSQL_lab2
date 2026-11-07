package org.config;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Precondition;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import lombok.extern.slf4j.Slf4j;
import org.models.OrderState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Profile("firestore-migration")
@Slf4j
public class OrderStatusStructureMigration implements ApplicationRunner {
    private static final String STATUS_LOG_COLLECTION = "OrderStatusLog";

    private final Firestore firestore;
    private final ConfigurableApplicationContext applicationContext;
    private final String ordersCollectionName;

    public OrderStatusStructureMigration(
            Firestore firestore,
            ConfigurableApplicationContext applicationContext,
            @Value("orders") String ordersCollectionName
    ) {
        this.firestore = firestore;
        this.applicationContext = applicationContext;
        this.ordersCollectionName = ordersCollectionName;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            final List<QueryDocumentSnapshot> orders = firestore.collection(ordersCollectionName)
                    .whereIn("status", List.of(
                            OrderState.PENDING.name(),
                            OrderState.IN_PROGRESS.name(),
                            OrderState.DONE.name(),
                            OrderState.ABORTED.name()
                    ))
                    .get()
                    .get()
                    .getDocuments();

            for (int start = 0; start < orders.size(); start += 500) {
                final WriteBatch batch = firestore.batch();
                final List<QueryDocumentSnapshot> part = orders.subList(start, Math.min(start + 500, orders.size()));

                for (QueryDocumentSnapshot order : part) {
                    final String status = order.getString("status");
                    final Timestamp changedAt = getLastStatusChange(order);
                    batch.update(
                            order.getReference(),
                            Map.of("statusObject", Map.of(
                                    "value", status,
                                    "changedAt", changedAt.toDate()
                            )),
                            Precondition.updatedAt(order.getUpdateTime())
                    );
                }

                batch.commit().get();
            }

            log.info("Order status migration completed: {} documents updated", orders.size());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order status migration was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("Unable to migrate order statuses", exception);
        } finally {
            applicationContext.close();
        }
    }

    private Timestamp getLastStatusChange(DocumentSnapshot order) throws InterruptedException, ExecutionException {
        final List<QueryDocumentSnapshot> logs = order.getReference()
                .collection(STATUS_LOG_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .get()
                .getDocuments();

        if (!logs.isEmpty() && logs.get(0).getTimestamp("timestamp") != null) {
            return logs.get(0).getTimestamp("timestamp");
        }

        return order.getTimestamp("createdAt");
    }
}
