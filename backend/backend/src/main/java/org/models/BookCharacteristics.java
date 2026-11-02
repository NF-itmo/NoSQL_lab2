package org.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCharacteristics {
    private String genre;
    private String publisher;
    private Integer publicationYear;
    private Integer pageCount;
}
