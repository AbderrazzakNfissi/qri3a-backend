package my.project.qri3a.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.NotificationResponseDTO;
import my.project.qri3a.entities.Notification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationMapper {

    // Assume you have an ImageMapper component that maps Image -> ImageResponseDTO.
    private final ImageMapper imageMapper;

    public NotificationResponseDTO toDTO(Notification notification) {
        if (notification == null) {
            log.warn("Notification is null, returning null DTO");
            return null;
        }

        log.info("Mapping Notification to NotificationResponseDTO, Notification ID: {}", notification.getId());

        LocalDateTime createdAt = notification.getCreatedAt();
        String createdAtStr = new Date().toString();
        if (createdAt != null) {
            // Convert LocalDateTime to ZonedDateTime with UTC timezone
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            // Define ISO 8601 format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Apply format and set in DTO
            createdAtStr = utcDateTime.format(formatter);
        }

        NotificationResponseDTO.NotificationResponseDTOBuilder builder = NotificationResponseDTO.builder()
                .id(notification.getId())
                .body(notification.getBody())
                .category(notification.getCategory())
                .productId(notification.getProduct() != null ? notification.getProduct().getId() : null)
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .read(notification.isRead())
                .createdAt(createdAtStr);

        if (notification.getProduct() != null
                && notification.getProduct().getImages() != null
                && !notification.getProduct().getImages().isEmpty()) {

            builder.firstProductImage(imageMapper.toDTO(notification.getProduct().getImages().get(0)));
        } else {
            log.info("Notification has no associated product images");
        }

        NotificationResponseDTO dto = builder.build();
        log.info("Mapped NotificationResponseDTO: {}", dto);
        return dto;
    }
}
