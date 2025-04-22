package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ScamType;
import my.project.qri3a.enums.ReportReason;
import my.project.qri3a.enums.ContactPreference;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamReportRequestDTO {

    // Information sur le produit signalé
    @NotBlank(message = "L'identifiant du produit est obligatoire")
    private String productIdentifier;


    @NotNull(message = "Le type de signalement est obligatoire")
    private ScamType type;

    @NotNull(message = "La raison du signalement est obligatoire")
    private ReportReason reportReason;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
    private String description;

    private String suspiciousListing;
    
    private BigDecimal amountLost;
    
    private LocalDate dateOfIncident;
    
    private List<String> attachmentTypes;
    
    private ContactPreference contactPreference;
    
    // Information sur le déclarant (obligatoire car toujours anonyme)
    @NotBlank(message = "Le nom du déclarant est obligatoire")
    private String reporterName;
    
    @NotBlank(message = "L'email du déclarant est obligatoire")
    @Email(message = "L'email du déclarant n'est pas valide")
    private String reporterEmail;
    
    private String reporterPhone;
}