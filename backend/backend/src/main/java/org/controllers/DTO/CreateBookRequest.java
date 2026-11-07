package org.controllers.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import org.controllers.DTO.internal.CreateBookCharacteristicsRequest;
import org.controllers.DTO.internal.CreateBookLocationRequest;

@Getter
public class CreateBookRequest {
    @NotNull(message = "Book id should be specified")
    @Positive(message = "Book id should be positive")
    private Integer id;

    @NotBlank(message = "Title should be specified")
    private String title;

    @NotBlank(message = "Author should be specified")
    private String author;

    @NotNull(message = "Location should be specified")
    @Valid
    private CreateBookLocationRequest location;

    @NotBlank(message = "Description should be specified")
    private String description;

    @NotNull(message = "Characteristics should be specified")
    @Valid
    private CreateBookCharacteristicsRequest characteristics;
}
