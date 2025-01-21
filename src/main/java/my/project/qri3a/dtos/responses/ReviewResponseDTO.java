package my.project.qri3a.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import my.project.qri3a.entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewResponseDTO {

    private UUID id;
    private String comment;
    private Float rating;
    @JsonProperty("userId")
    private UUID userId;
    private ReviewerResponseDTO reviewer;
    private String createdAt;
    private String updatedAt;
}