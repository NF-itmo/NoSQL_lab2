package org.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Pipeline;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.pipeline.stages.Search;
import lombok.extern.slf4j.Slf4j;
import org.models.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.google.cloud.firestore.pipeline.expressions.Expression.*;

@Repository
@Slf4j
public class BooksFirestoreRepository {
    private final Firestore firestore;
    private final String collectionName;
    private final ObjectMapper objectMapper;

    public BooksFirestoreRepository(
            Firestore firestore,
            ObjectMapper objectMapper,
            @Value("books") String collectionName
    ) {
        this.firestore = firestore;
        this.objectMapper = objectMapper;
        this.collectionName = collectionName;
    }

    public Optional<Book> getById(Integer id) {
        try {
            return Optional.ofNullable(
                    firestore.collection(collectionName)
                            .document(String.valueOf(id))
                            .get()
                            .get()
                            .toObject(Book.class)
            );
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while receiving book {}", id, e);
            throw new RuntimeException(e);
        }
    }

    public List<Book> get(Integer start, Integer limit) {
        try {
            return firestore.collection(collectionName)
                    .orderBy("id")
                    .offset(start)
                    .limit(limit)
                    .get()
                    .get()
                    .toObjects(Book.class);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while receiving books", e);
            throw new RuntimeException(e);
        }
    }


    public List<Book> search(
            String titleFragment,
            Boolean availabilityStatus,
            Integer start,
            Integer limit
    ) {
        Pipeline pipeline = firestore.pipeline().collection(collectionName);

        if (titleFragment != null && !titleFragment.isBlank()) {
            pipeline = pipeline.search(
                    Search.withQuery(documentMatches(titleFragment.trim()))
                            .withLanguageCode("ru")
                            .withSort(score().descending())
            );
        } else {
            pipeline = pipeline.sort(field("id").ascending());
        }

        if (availabilityStatus != null) {
            pipeline = pipeline.where(
                    field("available").equal(availabilityStatus)
            );
        }

        try {
            Pipeline.Snapshot snapshot = pipeline
                    .offset(start)
                    .limit(limit)
                    .execute()
                    .get();
            return snapshot.getResults().stream()
                    .map(result -> objectMapper.convertValue(
                            result.getData(),
                            Book.class
                    ))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while searching books", e);
            throw new RuntimeException(e);
        }
    }

    public void create(Book book) {
        firestore.collection(collectionName)
                .document(String.valueOf(book.getId()))
                .set(book);
    }

    public long incrementViews(Integer id) {
        try {
            return firestore.runTransaction(transaction ->
                            incrementViews(transaction, id)
                    ).get();
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Error while incrementing views for book {}", id, exception);
            throw new RuntimeException(exception);
        }
    }

    public void delete(Integer id) {
        firestore.collection(collectionName)
                .document(String.valueOf(id))
                .delete();
    }

    public void reserve(Integer id) {
        try {
            firestore.collection(collectionName)
                    .document(String.valueOf(id))
                    .update("available", false)
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error while reserving book {}", id, e);
            throw new RuntimeException(e);
        }
    }

    public void release(Integer id) {
        try {
            firestore.collection(collectionName)
                    .document(String.valueOf(id))
                    .update("available", true)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while releasing book {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private long incrementViews(Transaction transaction, Integer id) throws InterruptedException, ExecutionException {
        final DocumentReference bookDocument = firestore.collection(collectionName).document(String.valueOf(id));
        final DocumentSnapshot snapshot = transaction.get(bookDocument).get();

        final long currentViews = Optional.ofNullable(snapshot.getLong("views")).orElse(0L);
        final long nextViews = currentViews + 1;

        transaction.update(bookDocument, "views", nextViews);
        return nextViews;
    }
}
