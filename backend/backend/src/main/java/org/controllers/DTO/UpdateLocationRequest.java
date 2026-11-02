package org.controllers.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class UpdateLocationRequest {
    @NotNull(message = "Hall should be specified")
    @Positive(message = "Hall should be positive")
    private Integer hall;

    @NotNull(message = "Rack should be specified")
    @Positive(message = "Rack should be positive")
    private Integer rack;

    @NotNull(message = "Shelf should be specified")
    @Positive(message = "Shelf should be positive")
    private Integer shelf;
}
