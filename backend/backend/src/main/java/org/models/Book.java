package org.models;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "Books")
public class Book {
    @DocumentId
    private Integer id;

    private String title;
    private String author;
    private String genre;
    private String description;
    private Boolean available;

    @Transient
    private Long views;
}
