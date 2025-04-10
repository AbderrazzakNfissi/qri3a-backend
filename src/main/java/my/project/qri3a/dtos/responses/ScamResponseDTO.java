package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.enums.ScamType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamResponseDTO {

    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;

    private UUID productId;
    private String productTitle;
    private String productImageUrl;

    private UUID sellerId;
    private String sellerName;
    private String sellerEmail;

    private ScamType type;
    private String typeLabel;
    private String description;
    private ScamStatus status;
    private String statusLabel;
    private String adminComment;

    private UUID processedById;
    private String processedByName;
    private LocalDateTime processedAt;

    private String createdAt;
    private String updatedAt;
}
