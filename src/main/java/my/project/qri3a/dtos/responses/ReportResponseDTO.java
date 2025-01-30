package my.project.qri3a.dtos.responses;
import lombok.*;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDTO {

    private UUID id;
    private String reason;
}
