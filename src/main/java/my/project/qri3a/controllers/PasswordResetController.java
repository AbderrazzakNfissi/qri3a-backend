package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.PasswordResetDTO;
import my.project.qri3a.dtos.requests.PasswordResetRequestDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO requestDTO) {

        try {
            passwordResetService.requestPasswordReset(requestDTO.getEmail());

            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "Un email de réinitialisation a été envoyé si l'adresse existe dans notre base de données.",
                    HttpStatus.OK.value()
            );
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            // Pour des raisons de sécurité, on ne révèle pas si l'email existe ou non
            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "Un email de réinitialisation a été envoyé si l'adresse existe dans notre base de données.",
                    HttpStatus.OK.value()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la demande de réinitialisation de mot de passe", e);
            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "Une erreur s'est produite lors de la demande de réinitialisation.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetDTO resetDTO) {

        try {
            passwordResetService.resetPassword(resetDTO.getToken(), resetDTO.getPassword());

            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "Votre mot de passe a été réinitialisé avec succès.",
                    HttpStatus.OK.value()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe", e);
            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "Une erreur s'est produite lors de la réinitialisation du mot de passe.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}