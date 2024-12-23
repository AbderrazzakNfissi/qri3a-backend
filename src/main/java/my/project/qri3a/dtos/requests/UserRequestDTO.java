package my.project.qri3a.dtos.requests;
import jakarta.validation.constraints.*;
import my.project.qri3a.enums.Role;
import lombok.Data;

@Data
public class UserRequestDTO {

    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    private String phoneNumber;

    private String address;

    private Role role;

    private Float rating;
}
