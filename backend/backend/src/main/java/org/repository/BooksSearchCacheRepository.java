package org.repository;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.PutOption;
import org.models.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

@Repository
public class BooksSearchCacheRepository {
    private static final String SEARCH_CACHE_KEY_PREFIX = "/books/search/";

    private final KV kvClient;
    private final Lease leaseClient;
    private final ObjectMapper objectMapper;
    private final long cacheTtlSeconds;

    public BooksSearchCacheRepository(
            Client client,
            ObjectMapper objectMapper,
            @Value("${books-service.search-cache-ttl-seconds:60}") long cacheTtlSeconds
    ) {
        this.kvClient = client.getKVClient();
        this.leaseClient = client.getLeaseClient();
        this.objectMapper = objectMapper;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public CompletableFuture<Void> put(
            String titleFragment,
            Boolean availabilityStatus,
            Integer start,
            Integer limit,
            List<Book> books
    ) {
        final ByteSequence key = searchKey(titleFragment, availabilityStatus, start, limit);
        final ByteSequence value = serializeBooks(books);

        return leaseClient.grant(cacheTtlSeconds)
                .thenCompose(lease -> kvClient.put(
                        key,
                        value,
                        PutOption.builder().withLeaseId(lease.getID()).build()
                ))
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Optional<List<Book>>> get(
            String titleFragment,
            Boolean availabilityStatus,
            Integer start,
            Integer limit
    ) {
        final ByteSequence key = searchKey(
                titleFragment,
                availabilityStatus,
                start,
                limit
        );

        return kvClient.get(key).thenApply(response -> {
            if (response.getKvs().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(deserializeBooks(
                    response.getKvs().get(0).getValue().toString(UTF_8)
            ));
        });
    }

    public void invalidate() {
        final ByteSequence prefix = ByteSequence.from(SEARCH_CACHE_KEY_PREFIX, UTF_8);
        final DeleteOption option = DeleteOption.builder().isPrefix(true).build();

        kvClient.delete(prefix, option).thenApply(response -> null);
    }

    private ByteSequence searchKey(
            String titleFragment,
            Boolean availabilityStatus,
            Integer start,
            Integer limit
    ) {
        final SearchQuery query = new SearchQuery(
                SearchQuery.normalizeTitle(titleFragment),
                availabilityStatus,
                start,
                limit
        );
        final byte[] queryHash = query.sha256(this::serializeQuery);

        return ByteSequence.from(
                SEARCH_CACHE_KEY_PREFIX + HexFormat.of().formatHex(queryHash),
                UTF_8
        );
    }

    private ByteSequence serializeQuery(SearchQuery query) {
        try {
            return ByteSequence.from(objectMapper.writeValueAsString(query), UTF_8);
        } catch (JacksonException exception) {
            throw new CompletionException("Failed to serialize query", exception);
        }
    }

    private ByteSequence serializeBooks(List<Book> books) {
        try {
            return ByteSequence.from(objectMapper.writeValueAsString(books), UTF_8);
        } catch (JacksonException exception) {
            throw new CompletionException("Failed to serialize books", exception);
        }
    }

    private List<Book> deserializeBooks(String booksJson) {
        try {
            return Arrays.asList(objectMapper.readValue(booksJson, Book[].class));
        } catch (JacksonException exception) {
            throw new CompletionException("Failed to deserialize books", exception);
        }
    }

    private record SearchQuery(
            String titleFragment,
            Boolean availabilityStatus,
            Integer start,
            Integer limit
    ) {
        public byte[] sha256(Function<SearchQuery, ByteSequence> querySerializer) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(
                        querySerializer.apply(this).getBytes()
                );
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is not supported", exception);
            }
        }

        public static String normalizeTitle(String titleFragment) {
            if (titleFragment == null || titleFragment.isBlank()) {
                return null;
            }
            return titleFragment.trim().toLowerCase();
        }
    }
}
