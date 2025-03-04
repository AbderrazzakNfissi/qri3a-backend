package my.project.qri3a.dtos.responses;
import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;
import my.project.qri3a.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private ProductCategory category;
    private String city;
    private String phone;
    private ProductCondition condition;
    private ProductStatus status;
    private UserDTO user;
    private List<ImageResponseDTO> images;
    private String createdAt;
}
