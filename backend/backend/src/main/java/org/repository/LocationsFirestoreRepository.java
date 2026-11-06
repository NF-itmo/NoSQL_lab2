package org.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.models.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class LocationsFirestoreRepository {
    private final Firestore firestore;
    private final String collectionName;

    public LocationsFirestoreRepository(
            Firestore firestore,
            @Value("locations") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
    }

    public Location getOrCreate(Location location) {
        try {
            return firestore.runTransaction(transaction -> {
                final QuerySnapshot locations = transaction.get(
                        firestore.collection(collectionName)
                                .whereEqualTo("hall", location.getHall())
                                .whereEqualTo("rack", location.getRack())
                                .whereEqualTo("shelf", location.getShelf())
                                .limit(1)
                ).get();

                if (!locations.isEmpty()) {
                    return locations.getDocuments().get(0).toObject(Location.class);
                }

                location.setId(UUID.randomUUID().toString());
                transaction.create(
                        firestore.collection(collectionName).document(location.getId()),
                        location
                );
                return location;
            }).get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while creating location", exception);
            throw new RuntimeException(exception);
        }
    }

    public Optional<Location> update(String id, Location location) {
        try {
            final var document = firestore.collection(collectionName).document(id);
            if (!document.get().get().exists()) {
                return Optional.empty();
            }

            location.setId(id);
            document.set(location).get();
            return Optional.of(location);
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while updating location {}", id, exception);
            throw new RuntimeException(exception);
        }
    }

    public Optional<Location> getById(String id) {
        try {
            return Optional.ofNullable(
                    firestore.collection(collectionName)
                            .document(id)
                            .get()
                            .get()
                            .toObject(Location.class)
            );
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving location {}", id, exception);
            throw new RuntimeException(exception);
        }
    }

    public List<Location> getAll() {
        try {
            return firestore.collection(collectionName)
                    .orderBy("hall")
                    .get()
                    .get()
                    .toObjects(Location.class);
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while receiving locations", exception);
            throw new RuntimeException(exception);
        }
    }
}
