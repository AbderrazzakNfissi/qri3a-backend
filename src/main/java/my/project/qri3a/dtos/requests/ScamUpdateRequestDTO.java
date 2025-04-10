package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.ScamStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamUpdateRequestDTO {

    @NotNull(message = "Le statut est obligatoire")
    private ScamStatus status;

    private String adminComment;
}