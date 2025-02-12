package my.project.qri3a.dtos.responses;

import lombok.*;
import my.project.qri3a.enums.ProductCategory;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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
