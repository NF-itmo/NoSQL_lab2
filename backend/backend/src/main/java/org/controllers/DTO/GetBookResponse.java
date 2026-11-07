package org.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.controllers.DTO.internal.GetBookCharacteristicsResponse;
import org.models.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetBookResponse {
    private Integer id;
    private String title;
    private String author;
    private Location location;
    private String description;
    private Boolean available;
    private Long views;
    private GetBookCharacteristicsResponse characteristics;
}
