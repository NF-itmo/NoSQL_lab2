package org.controllers.DTO.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetBookCharacteristicsResponse {
    private String genre;
    private String publisher;
    private Integer publicationYear;
    private Integer pageCount;
}
