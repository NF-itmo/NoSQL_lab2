package org.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetBookStatusAnalyticsResponse {
    private Integer bookId;
    private String bookTitle;
    private long pendingCount;
    private long inProgressCount;
    private long doneCount;
    private long abortedCount;
}
