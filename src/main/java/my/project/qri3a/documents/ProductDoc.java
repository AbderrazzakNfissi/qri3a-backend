package my.project.qri3a.documents;

import lombok.*;
import my.project.qri3a.entities.Image;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Document(indexName = "products_idx")
public class ProductDoc {
    @Id
    private UUID id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    private BigDecimal price;
    private String location;
    private String city;
    private String category;
    private String condition;
    private String createdAt;
    // Nouveaux champs
    private String firstImageUrl;
    private int numberOfImages;
}
