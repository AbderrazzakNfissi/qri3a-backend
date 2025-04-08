package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ContactStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponseDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String reason;
    private String message;
    private ContactStatus status;
    private String createdAt;
    private String updatedAt;
}
