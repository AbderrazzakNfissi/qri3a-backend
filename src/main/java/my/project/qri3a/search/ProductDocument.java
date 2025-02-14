package my.project.qri3a.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private String city;
    private String category;
    private String condition;

    // Méthode de conversion depuis l'entité Product
    public static ProductDocument fromProduct(my.project.qri3a.entities.Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setTitle(product.getTitle());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice());
        doc.setLocation(product.getLocation());
        doc.setCity(product.getCity());
        doc.setCategory(product.getCategory().toString());
        doc.setCondition(product.getCondition().toString());
        return doc;
    }
}
