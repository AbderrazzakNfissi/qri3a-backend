package my.project.qri3a.dtos.responses;

import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private ProductCategory category;
    private ProductCondition condition;
    private UUID sellerId;
    private String sellerName;
    private String createdAt;
    private String updatedAt;
}
