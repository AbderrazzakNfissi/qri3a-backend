package my.project.qri3a.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewResponseDTO {

    private UUID id;
    private String comment;
    private Float rating;

    @JsonProperty("userId")
    private UUID userId;

    private UUID reviewerId;
    private String createdAt;
    private String updatedAt;
}