package my.project.qri3a.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.NotificationResponseDTO;
import my.project.qri3a.entities.Notification;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationMapper {

    // Composant pour mapper une image en ImageResponseDTO
    private final ImageMapper imageMapper;

    public NotificationResponseDTO toDTO(Notification notification) {
        if (notification == null) {
            log.warn("Notification is null, returning null DTO");
            return null;
        }

        log.info("Mapping Notification to NotificationResponseDTO, Notification ID: {}", notification.getId());

        // Instanciation d'un DTO vide
        NotificationResponseDTO dto = new NotificationResponseDTO();

        // Copie des propriétés communes depuis l'entité vers le DTO.
        // On ignore "product", "user", "createdAt" et "firstProductImage" car elles nécessitent un traitement spécifique.
        BeanUtils.copyProperties(notification, dto, "product", "user", "createdAt", "firstProductImage");

        // Affecter manuellement le productId et le userId
        if (notification.getProduct() != null) {
            dto.setProductId(notification.getProduct().getId());
        }
        if (notification.getUser() != null) {
            dto.setUserId(notification.getUser().getId());
        }

        // Formatage du champ createdAt en ISO 8601 (en UTC)
        LocalDateTime createdAt = notification.getCreatedAt();
        String createdAtStr = "";
        if (createdAt != null) {
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            createdAtStr = utcDateTime.format(formatter);
        }
        dto.setCreatedAt(createdAtStr);

        // Si le produit associé possède des images, mapper la première image
        if (notification.getProduct() != null
                && notification.getProduct().getImages() != null
                && !notification.getProduct().getImages().isEmpty()) {
            dto.setFirstProductImage(imageMapper.toDTO(notification.getProduct().getImages().get(0)));
        } else {
            log.info("Notification has no associated product images");
        }

        log.info("Mapped NotificationResponseDTO: {}", dto);
        return dto;
    }
}
