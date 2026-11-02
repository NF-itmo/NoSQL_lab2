package org.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.models.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetBookItemResponse {
    private Integer id;
    private String title;
    private String author;
    private String locationId;
    private Location location;
    private Boolean available;
}
