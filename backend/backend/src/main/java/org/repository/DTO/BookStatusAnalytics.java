package org.repository.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.models.OrderState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStatusAnalytics {
    private Integer bookId;
    private String bookTitle;
    private long pendingCount;
    private long inProgressCount;
    private long doneCount;
    private long abortedCount;

    public long getCount(OrderState state) {
        return switch (state) {
            case PENDING -> pendingCount;
            case IN_PROGRESS -> inProgressCount;
            case DONE -> doneCount;
            case ABORTED -> abortedCount;
        };
    }
}
