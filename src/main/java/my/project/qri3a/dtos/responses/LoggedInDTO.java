package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoggedInDTO {

    private UUID id;
    private String email;
    private String name;
    private String token;

}
