package org.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetLocationResponse {
    private String id;
    private Integer hall;
    private Integer rack;
    private Integer shelf;
}
