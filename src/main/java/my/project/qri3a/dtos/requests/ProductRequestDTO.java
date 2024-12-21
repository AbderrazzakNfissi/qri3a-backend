package my.project.qri3a.dtos.requests;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequestDTO {

    @NotBlank(message = "Title is mandatory")
    private String title;

    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;

    @NotBlank(message = "Location is mandatory")
    private String location;

    @NotBlank(message = "phone is mandatory")
    private String phone;

    @NotBlank(message = "City is mandatory")
    private String city;

    @NotNull(message = "Category is mandatory")
    private ProductCategory category;

    @NotNull(message = "Condition is mandatory")
    private ProductCondition condition;

    @NotNull(message = "Seller ID is mandatory")
    private UUID sellerId;
}
