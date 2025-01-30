package my.project.qri3a.dtos.requests;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDTO {

    @NotBlank(message = "Reason is mandatory")
    private String reason;
}
