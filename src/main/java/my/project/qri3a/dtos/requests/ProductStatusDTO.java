package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ProductStatus;

/**
 * DTO for product status updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusDTO {

    @NotNull(message = "Status is required")
    private ProductStatus status;

    private String rejectionComment;
}