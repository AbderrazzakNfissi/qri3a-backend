package my.project.qri3a.dtos.responses;

import lombok.Data;
import my.project.qri3a.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewerResponseDTO {
    private UUID id;
    private String name;
    //Photo de profile a ajoute par la suite ...
}


