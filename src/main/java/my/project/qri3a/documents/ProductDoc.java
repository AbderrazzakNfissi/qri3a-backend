package my.project.qri3a.documents;

import java.math.BigDecimal;
import java.util.UUID;

import my.project.qri3a.enums.ProductStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


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
    private String status;
    private String createdAt;
    // Nouveaux champs
    private String firstImageUrl;
    private int numberOfImages;

    private String delivery;
    private BigDecimal deliveryFee;
    private Boolean deliveryAllMorocco;
    private String deliveryZones;
    private String deliveryTime;

}
