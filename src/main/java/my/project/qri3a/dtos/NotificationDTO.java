package my.project.qri3a.dtos;

import lombok.Data;
import my.project.qri3a.enums.ProductCategory;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class NotificationDTO {
    private UUID id;
    private String body;
    private LocalDateTime createdAt;
    private ProductCategory category;
    private UUID productId;
    private UUID userId;
    private boolean read;
}
