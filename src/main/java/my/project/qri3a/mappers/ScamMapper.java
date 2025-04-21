package my.project.qri3a.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.entities.Scam;
import my.project.qri3a.enums.ScamStatus;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScamMapper {

    /**
     * Convertit un DTO de demande en entité Scam (pour signalements anonymes uniquement)
     * 
     * @param dto Le DTO de demande
     * @return L'entité Scam créée
     */
    public Scam toEntity(ScamReportRequestDTO dto) {
        Scam scam = new Scam();

        // Informations sur le produit signalé
        scam.setProductIdentifier(dto.getProductIdentifier());
        scam.setProductTitle(dto.getProductTitle());

        // Set values directly from DTO
        scam.setType(dto.getType());
        scam.setReportReason(dto.getReportReason());
        scam.setDescription(dto.getDescription());
        scam.setSuspiciousListing(dto.getSuspiciousListing());
        scam.setAmountLost(dto.getAmountLost());
        scam.setDateOfIncident(dto.getDateOfIncident());
        scam.setAttachmentTypes(dto.getAttachmentTypes());
        scam.setContactPreference(dto.getContactPreference());

        // Set reporter info (toujours requis en mode anonyme)
        scam.setReporterName(dto.getReporterName());
        scam.setReporterEmail(dto.getReporterEmail());
        scam.setReporterPhone(dto.getReporterPhone());

        // Set default values
        scam.setStatus(ScamStatus.PENDING);

        return scam;
    }

    /**
     * Convertit une entité Scam en DTO de réponse
     * 
     * @param scam L'entité Scam à convertir
     * @return Le DTO de réponse créé
     */
    public ScamResponseDTO toDTO(Scam scam) {
        if (scam == null) {
            return null;
        }

        ScamResponseDTO dto = new ScamResponseDTO();

        // Copy basic properties
        BeanUtils.copyProperties(scam, dto, "processedBy");
        
        // Map admin information
        if (scam.getProcessedBy() != null) {
            dto.setProcessedById(scam.getProcessedBy().getId());
            dto.setProcessedByName(scam.getProcessedBy().getName());
        }

        // Map enum labels
        if (scam.getType() != null) {
            dto.setTypeLabel(scam.getType().toString());
        }
        
        if (scam.getReportReason() != null) {
            dto.setReportReasonLabel(scam.getReportReason().toString());
        }

        if (scam.getStatus() != null) {
            dto.setStatusLabel(scam.getStatus().toString());
        }

        // Set product information
        dto.setProductId(scam.getProductIdentifier());
        dto.setProductTitle(scam.getProductTitle());

        // Reporter information (always anonymous now)
        dto.setReporterName(scam.getReporterName());
        dto.setReporterEmail(scam.getReporterEmail());
        dto.setReporterPhone(scam.getReporterPhone());

        LocalDateTime scamCreatedAt= scam.getCreatedAt();
        if (scamCreatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = scamCreatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        LocalDateTime scamUpdatedAt= scam.getUpdatedAt();
        if (scamUpdatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = scamUpdatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setUpdatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }
}