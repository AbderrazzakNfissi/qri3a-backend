package my.project.qri3a.dtos.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageResponseDTO {

    private UUID id;
    private String url;
    private LocalDateTime createdAt;
}
