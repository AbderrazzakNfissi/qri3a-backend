package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ContactPreference;
import my.project.qri3a.enums.ReportReason;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.enums.ScamType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamResponseDTO {

    private UUID id;
    
    // Informations du déclarant anonyme
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;

    // Informations du produit signalé (identifiant sous forme de String maintenant)
    private String productId;
    private String productTitle;
    
    // Informations sur le signalement
    private ScamType type;
    private String typeLabel;
    
    private ReportReason reportReason;
    private String reportReasonLabel;
    
    private String description;
    private String suspiciousListing;
    private BigDecimal amountLost;
    private LocalDate dateOfIncident;
    private List<String> attachmentTypes;
    private ContactPreference contactPreference;
    
    // Informations sur le traitement du signalement
    private ScamStatus status;
    private String statusLabel;
    private String adminComment;

    private UUID processedById;
    private String processedByName;
    private LocalDateTime processedAt;

    private String createdAt;
    private String updatedAt;
}
