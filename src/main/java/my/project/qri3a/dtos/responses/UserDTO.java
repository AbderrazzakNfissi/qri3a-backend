package my.project.qri3a.dtos.responses;

import lombok.Data;
import my.project.qri3a.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String name;
    private String phoneNumber;
    private Float rating;
}
