package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @Pattern(regexp = "^(\\+\\d{1,3})?\\s?\\d{9,10}$", message = "Format de numéro de téléphone invalide")
    private String phone;

    @NotBlank(message = "La raison du contact est obligatoire")
    @Size(min = 10, max = 100, message = "Le message doit contenir entre 10 et 100 caractères")
    private String reason;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 500, message = "Le message doit contenir entre 10 et 500 caractères")
    private String message;
}