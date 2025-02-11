package my.project.qri3a.dtos.requests;

import lombok.Data;
import my.project.qri3a.enums.ProductCategory;
import java.util.UUID;

@Data
public class NotificationRequestDTO {
    private String body;
    private ProductCategory category;
    private UUID productId;
    private UUID userId;
}
