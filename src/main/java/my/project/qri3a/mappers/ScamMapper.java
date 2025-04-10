package my.project.qri3a.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

import my.project.qri3a.enums.ScamStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.entities.Image;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.Scam;
import my.project.qri3a.entities.User;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScamMapper {

    public Scam toEntity(ScamReportRequestDTO dto, User reporter, Product product) {
        Scam scam = new Scam();

        // Set values directly from DTO
        scam.setType(dto.getType());
        scam.setDescription(dto.getDescription());

        // Set referenced entities
        scam.setReporter(reporter);
        scam.setReportedProduct(product);

        // Set default values
        scam.setStatus(ScamStatus.PENDING);

        return scam;
    }

    public ScamResponseDTO toDTO(Scam scam) {
        if (scam == null) {
            return null;
        }

        ScamResponseDTO dto = new ScamResponseDTO();

        // Copy basic properties
        BeanUtils.copyProperties(scam, dto, "reporter", "reportedProduct", "processedBy");

        // Map reporter information
        if (scam.getReporter() != null) {
            dto.setReporterId(scam.getReporter().getId());
            dto.setReporterName(scam.getReporter().getName());
            dto.setReporterEmail(scam.getReporter().getEmail());
        }

        // Map product information
        if (scam.getReportedProduct() != null) {
            Product product = scam.getReportedProduct();
            dto.setProductId(product.getId());
            dto.setProductTitle(product.getTitle());

            // Extract first image URL
            String imageUrl = extractFirstImageUrl(product);
            dto.setProductImageUrl(imageUrl);

            // Map seller information if available
            if (product.getSeller() != null) {
                dto.setSellerId(product.getSeller().getId());
                dto.setSellerName(product.getSeller().getName());
                dto.setSellerEmail(product.getSeller().getEmail());
            }
        }

        // Map admin information
        if (scam.getProcessedBy() != null) {
            dto.setProcessedById(scam.getProcessedBy().getId());
            dto.setProcessedByName(scam.getProcessedBy().getName());
        }

        // Map enum labels
        if (scam.getType() != null) {
            dto.setTypeLabel(scam.getType().getLabel());
        }

        if (scam.getStatus() != null) {
            dto.setStatusLabel(scam.getStatus().getLabel());
        }

        LocalDateTime scamCreatedAt= scam.getCreatedAt();
        if (scamCreatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = scamCreatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        LocalDateTime scamUpdatedAt= scam.getCreatedAt();
        if (scamCreatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = scamUpdatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setUpdatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }

    private String extractFirstImageUrl(Product product) {
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            // Get first image sorted by order (similar to ProductMapper approach)
            return product.getImages().stream()
                    .sorted(Comparator.comparingInt(Image::getOrder))
                    .findFirst()
                    .map(Image::getUrl)
                    .orElse(null);
        }
        return null;
    }
}