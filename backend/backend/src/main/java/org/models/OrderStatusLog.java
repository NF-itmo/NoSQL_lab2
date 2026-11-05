package org.models;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "OrderStatusLog")
public class OrderStatusLog {
    @DocumentId
    private String id;
    private Integer bookId;
    private String bookTitle;
    private OrderState status;
    private Instant timestamp;
}
