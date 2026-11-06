package org.services;

import org.exceptions.NotFoundException;
import org.models.Book;
import org.repository.BooksFirestoreRepository;
import org.repository.BooksSearchCacheRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BooksService {
    private final BooksSearchCacheRepository booksSearchCacheRepository;
    private final BooksFirestoreRepository booksRepository;
    public BooksService(
            BooksFirestoreRepository booksRepository,
            BooksSearchCacheRepository booksSearchCacheRepository
    ) {
        this.booksSearchCacheRepository = booksSearchCacheRepository;
        this.booksRepository = booksRepository;
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

        List<Book> result = booksRepository.search(title, available, start, limit);
        booksSearchCacheRepository.put(title, available, start, limit, result).join();

        return result;
    }

    public Book getById(Integer bookId) {
        return booksRepository.getById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    public Book getDetailsById(Integer bookId) {
        final Book book = getById(bookId);
        book.setViews(booksRepository.incrementViews(bookId));

        return book;
    }

    public Book create(Book book) {
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
}
