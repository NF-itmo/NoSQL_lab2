package org.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetOrderStatusAnalyticsResponse {
    private List<GetBookStatusAnalyticsResponse> books;
}
