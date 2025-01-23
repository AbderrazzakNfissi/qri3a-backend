package my.project.qri3a.dtos.responses;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewResponseDTO {

    private UUID id;
    private String comment;
    private Float rating;
    private ReviewerResponseDTO reviewer;
    private String createdAt;
    private String updatedAt;
}