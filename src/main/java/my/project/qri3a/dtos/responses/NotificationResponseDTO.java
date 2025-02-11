package my.project.qri3a.dtos.responses;

import lombok.Builder;
import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import java.time.LocalDateTime;
import java.util.UUID;

// Make sure to import your ImageResponseDTO
import my.project.qri3a.dtos.responses.ImageResponseDTO;

@Data
@Builder
public class NotificationResponseDTO {
    private UUID id;
    private String body;
    private ProductCategory category;
    private UUID productId;
    private UUID userId;
    private boolean read;
    private String createdAt;

    // New field for the first image of the product
    private ImageResponseDTO firstProductImage;
}
