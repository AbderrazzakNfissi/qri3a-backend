package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    @NotBlank(message = "Mot de passe actuel est requis.")
    private String currentPassword;

    @NotBlank(message = "Nouveau mot de passe est requis.")
    @Size(min = 8, message = "Nouveau mot de passe doit contenir au moins 8 caractères.")
    private String newPassword;

    @NotBlank(message = "Confirmer le mot de passe est requis.")
    private String confirmPassword;
}
