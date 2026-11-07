package org.controllers.DTO.internal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateBookCharacteristicsRequest {
    private String genre;

    @Size(message = "Publisher should be shorter than 100 characters")
    private String publisher;

    @PositiveOrZero(message = "Publication year should not be negative")
    private Integer publicationYear;

    @Positive(message = "Page count should be positive")
    private Integer pageCount;
}
