package org.models;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "locations")
public class Location {
    @DocumentId
    private String id;

    private Integer hall;
    private Integer rack;
    private Integer shelf;
}
