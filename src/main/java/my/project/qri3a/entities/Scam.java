package my.project.qri3a.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ContactPreference;
import my.project.qri3a.enums.ReportReason;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.enums.ScamType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scam {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Identifiant du produit signalé (stocké sous forme de chaîne)
    @NotBlank(message = "L'ID du produit est obligatoire")
    private String productIdentifier;
    
    // Titre du produit signalé (pour faciliter l'affichage sans avoir à charger le produit)
    private String productTitle;

    @NotNull(message = "Le type de signalement est obligatoire")
    @Enumerated(EnumType.STRING)
    private ScamType type;
    
    @NotNull(message = "La raison du signalement est obligatoire")
    @Enumerated(EnumType.STRING)
    private ReportReason reportReason;

    @NotBlank(message = "La description est obligatoire")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String suspiciousListing;
    
    private BigDecimal amountLost;
    
    private LocalDate dateOfIncident;
    
    @ElementCollection
    @CollectionTable(name = "scam_attachment_types", joinColumns = @JoinColumn(name = "scam_id"))
    @Column(name = "attachment_type")
    private List<String> attachmentTypes;
    
    @Enumerated(EnumType.STRING)
    private ContactPreference contactPreference;
    
    // Information sur le reporter (toujours anonyme)
    @NotBlank(message = "Le nom du déclarant est obligatoire")
    private String reporterName;
    
    @NotBlank(message = "L'email du déclarant est obligatoire")
    @Email
    private String reporterEmail;
    
    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    private ScamStatus status = ScamStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminComment;

    // Administrateur ayant traité le signalement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JsonBackReference
    private User processedBy;

    private LocalDateTime processedAt;

    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}