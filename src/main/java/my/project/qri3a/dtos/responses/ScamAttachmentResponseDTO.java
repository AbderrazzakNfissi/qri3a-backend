package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.entities.ScamAttachment.AttachmentType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamAttachmentResponseDTO {

    private UUID id;
    private UUID scamId;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private AttachmentType type;
    private String typeLabel;
    private LocalDateTime uploadedAt;
}