package my.project.qri3a.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class UserSettingsInfosDTO {

    private UUID id;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Le numéro de téléphone est invalide")
    private String phoneNumber;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
    //@Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$", message = "La ville contient des caractères invalides")
    private String city;

    private String aboutMe;

    private String website;

    private MultipartFile multipartFile;

    private String profileImage;

    private boolean removeProfileImage;
}