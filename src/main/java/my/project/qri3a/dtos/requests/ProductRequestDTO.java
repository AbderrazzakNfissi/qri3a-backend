package my.project.qri3a.dtos.requests;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;

@Data
public class ProductRequestDTO {

    @NotBlank(message = "Title is mandatory")
    private String title;

    @Size(max = 60000, message = "Description must not exceed 5000 characters")
    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;


    private String location;

    @NotBlank(message = "phone is mandatory")
    private String phone;

    @NotBlank(message = "City is mandatory")
    private String city;

    @NotNull(message = "Category is mandatory")
    private ProductCategory category;

    @NotNull(message = "Condition is mandatory")
    private ProductCondition condition;

    private String longitude;

    private String latitude;

    //@NotNull(message = "Seller ID is mandatory")
    // private UUID sellerId;

    private String delivery;
    private BigDecimal deliveryFee;
    private Boolean deliveryAllMorocco;
    private List<String> deliveryZones;
    private String deliveryTime;
}