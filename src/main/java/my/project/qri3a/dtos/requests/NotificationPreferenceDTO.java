package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDTO {

    private ProductCategory productCategory;

    private ProductCondition productState;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum price must be non-negative")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum price must be non-negative")
    private BigDecimal maxPrice;

    private String city;

    private boolean receiveEmails;
}