package org.services;

import org.exceptions.NotFoundException;
import org.models.Book;
import org.models.Location;
import org.repository.BooksFirestoreRepository;
import org.repository.BooksSearchCacheRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BooksService {
    private final BooksSearchCacheRepository booksSearchCacheRepository;
    private final BooksFirestoreRepository booksRepository;
    private final LocationsService locationsService;

    public BooksService(
            BooksFirestoreRepository booksRepository,
            BooksSearchCacheRepository booksSearchCacheRepository,
            LocationsService locationsService
    ) {
        this.booksSearchCacheRepository = booksSearchCacheRepository;
        this.booksRepository = booksRepository;
        this.locationsService = locationsService;
    }

    public List<Book> getAll(
            Integer start,
            Integer limit,
            String title,
            Boolean available
    ) {
        final Optional<List<Book>> cacheResult = booksSearchCacheRepository.get(
                title,
                available,
                start,
                limit
        ).join();

        if (cacheResult.isPresent()) {
            return cacheResult.get();
        }

        List<Book> result = setLocations(booksRepository.search(title, available, start, limit));
        booksSearchCacheRepository.put(title, available, start, limit, result).join();

        return result;
    }

    public Book getById(Integer bookId) {
        return setLocation(booksRepository.getById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found")));
    }

    public Book getDetailsById(Integer bookId) {
        final Book book = getById(bookId);
        book.setViews(booksRepository.incrementViews(bookId));

        return book;
    }

    public Book create(Book book) {
        final Location location = locationsService.getById(book.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found"));
        book.setLocation(location);
        booksRepository.create(book);
        booksSearchCacheRepository.invalidate();
        return book;
    }

    public void delete(Integer bookId) {
        getById(bookId);
        booksRepository.delete(bookId);
        booksSearchCacheRepository.invalidate();
    }

    public void release(Integer bookId) {
        booksRepository.release(bookId);
    }

    private List<Book> setLocations(List<Book> books) {
        return books.stream()
                .map(this::setLocation)
                .toList();
    }

    private Book setLocation(Book book) {
        if (book.getLocationId() == null) {
            return book;
        }

        locationsService.getById(book.getLocationId())
                .ifPresent(book::setLocation);
        return book;
    }
}
