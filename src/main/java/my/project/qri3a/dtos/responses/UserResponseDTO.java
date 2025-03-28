package my.project.qri3a.dtos.responses;
import java.time.LocalDateTime;
import java.util.UUID;
import my.project.qri3a.enums.Role;
import lombok.Data;

@Data
public class UserResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private Role role;
    private Long totalProducts;
    private String profileImage;
    private String createdAt;
    private boolean blocked;
    private String city;
    private String aboutMe;
    private String website;
    private String description;
}
