package my.project.qri3a.dtos.requests;
import jakarta.validation.constraints.*;
import my.project.qri3a.enums.Role;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9 .()-]{7,25}$", message = "Le numéro de téléphone est invalide")
    private String phoneNumber;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    @Min(value = 0, message = "La note doit être au moins 0")
    @Max(value = 5, message = "La note doit être au plus 5")
    private Float rating;
}
