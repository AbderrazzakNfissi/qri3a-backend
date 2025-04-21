package my.project.qri3a.mappers;

import my.project.qri3a.dtos.responses.ScamAttachmentResponseDTO;
import my.project.qri3a.entities.ScamAttachment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScamAttachmentMapper {

    /**
     * Convertit une entité ScamAttachment en DTO.
     *
     * @param attachment L'entité à convertir
     * @return Le DTO correspondant
     */
    public ScamAttachmentResponseDTO toDTO(ScamAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return ScamAttachmentResponseDTO.builder()
                .id(attachment.getId())
                .scamId(attachment.getScam().getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .type(attachment.getType())
                .typeLabel(attachment.getType() != null ? attachment.getType().getCode() : null)
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    /**
     * Convertit une liste d'entités ScamAttachment en liste de DTOs.
     *
     * @param attachments La liste d'entités à convertir
     * @return La liste de DTOs correspondants
     */
    public List<ScamAttachmentResponseDTO> toDTOList(List<ScamAttachment> attachments) {
        if (attachments == null) {
            return null;
        }
        
        return attachments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}