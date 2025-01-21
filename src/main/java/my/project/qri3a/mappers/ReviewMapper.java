package my.project.qri3a.mappers;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.requests.UpdateReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.entities.Review;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Component
public class ReviewMapper {

    /**
     * Converts a Review entity to a ReviewResponseDTO.
     *
     * @param review The Review entity to convert.
     * @return The corresponding ReviewResponseDTO.
     */
    public ReviewResponseDTO toDTO(Review review) {
        if (review == null) {
            return null;
        }

        ReviewResponseDTO dto = new ReviewResponseDTO();

        // Copy basic properties except for user and date fields
        BeanUtils.copyProperties(review, dto, "user");

        // Set userId
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
        }

        if(review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
        }

        LocalDateTime createdAt = review.getCreatedAt();
        if (createdAt != null) {
            // Convert LocalDateTime to ZonedDateTime with UTC timezone
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            // Define ISO 8601 format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Apply format and set in DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        LocalDateTime updatedAt = review.getUpdatedAt();
        if (updatedAt != null) {
            // Convert LocalDateTime to ZonedDateTime with UTC timezone
            ZonedDateTime utcDateTime = updatedAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            // Define ISO 8601 format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Apply format and set in DTO
            dto.setUpdatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }

    /**
     * Converts a ReviewRequestDTO to a Review entity.
     *
     * @param dto The ReviewRequestDTO to convert.
     * @return The corresponding Review entity.
     */
    public Review toEntity(ReviewRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Review review = new Review();

        // Copy properties from DTO to entity
        BeanUtils.copyProperties(dto, review, "user");

        // Note: User association should be handled separately
        // Typically set in the service layer

        return review;
    }

    /**
     * Updates an existing Review entity with data from a ReviewRequestDTO.
     *
     * @param dto    The ReviewRequestDTO containing updated data.
     * @param entity The Review entity to update.
     */
    public void updateEntityFromDTO(UpdateReviewRequestDTO dto, Review entity) {
        if (dto == null || entity == null) {
            return;
        }

        // Copy properties from DTO to entity, excluding immutable fields
        BeanUtils.copyProperties(dto, entity, "id", "user", "createdAt", "updatedAt");
    }

    /**
     * Formats a LocalDateTime to an ISO 8601 UTC string.
     *
     * @param dateTime The LocalDateTime to format.
     * @return The formatted date string.
     */
    private String formatDate(java.time.LocalDateTime dateTime) {
        ZonedDateTime utcDateTime = dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        return formatter.format(utcDateTime);
    }
}
