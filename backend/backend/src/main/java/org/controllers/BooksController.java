package org.controllers;

import jakarta.validation.Valid;
import org.controllers.DTO.CreateBookRequest;
import org.controllers.DTO.GetBookResponse;
import org.controllers.DTO.internal.CreateBookCharacteristicsRequest;
import org.controllers.DTO.internal.GetBookCharacteristicsResponse;
import org.models.Book;
import org.models.BookCharacteristics;
import org.services.BooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BooksController {
    private final BooksService booksService;

    public BooksController(BooksService booksService) {
        this.booksService = booksService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetBookResponse> createBook(
            @RequestBody @Valid final CreateBookRequest request
    ) {
        final Book book = booksService.create(Book.builder()
                .id(request.getId())
                .title(request.getTitle())
                .author(request.getAuthor())
                .locationId(request.getLocationId())
                .description(request.getDescription())
                .available(true)
                .views(0L)
                .characteristics(toModel(request.getCharacteristics()))
                .build());

        return ResponseEntity.ok(
                toResponse(book)
        );
    }

    @GetMapping
    public List<GetBookResponse> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false, defaultValue = "0") Integer start,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return booksService.getAll(start, limit, title, available).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{bookId}")
    public GetBookResponse getBook(
            @PathVariable Integer bookId
    ) {
        final Book book = booksService.getDetailsById(bookId);

        return toResponse(book);
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Integer bookId
    ) {
        booksService.delete(bookId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseBook(
            @PathVariable Integer bookId
    ) {
        booksService.release(bookId);

        return ResponseEntity.noContent().build();
    }

    private GetBookResponse toResponse(Book book) {
        return new GetBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getLocationId(),
                book.getLocation(),
                book.getDescription(),
                book.getAvailable(),
                book.getViews(),
                toResponse(book.getCharacteristics())
        );
    }

    private BookCharacteristics toModel(CreateBookCharacteristicsRequest request) {
        return BookCharacteristics.builder()
                .genre(request.getGenre())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .pageCount(request.getPageCount())
                .build();
    }

    private GetBookCharacteristicsResponse toResponse(BookCharacteristics characteristics) {
        if (characteristics == null) {
            return null;
        }

        return new GetBookCharacteristicsResponse(
                characteristics.getGenre(),
                characteristics.getPublisher(),
                characteristics.getPublicationYear(),
                characteristics.getPageCount()
        );
    }
}
