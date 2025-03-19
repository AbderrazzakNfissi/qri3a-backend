package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.VerificationRequest;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.TooManyAttemptsException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.EmailVerificationService;
import my.project.qri3a.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/verify")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(Authentication authentication) throws TooManyAttemptsException {
        try {
            User user = userService.getUserMe(authentication);

            if (user.isEmailVerified()) {
                return ResponseEntity.ok(new ApiResponse<>(null, "L'adresse email est déjà vérifiée.", HttpStatus.OK.value()));
            }

            emailVerificationService.sendVerificationCode(user);

            return ResponseEntity.ok(new ApiResponse<>(null, "Un code de vérification a été envoyé à votre adresse email.", HttpStatus.OK.value()));
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code de vérification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Une erreur s'est produite lors de l'envoi du code de vérification.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@RequestBody VerificationRequest request) throws TooManyAttemptsException{
        try {
            boolean verified = emailVerificationService.verifyCode(request.getCode(), request.getUserId());

            if (verified) {
                return ResponseEntity.ok(new ApiResponse<>(null, "Votre adresse email a été vérifiée avec succès.", HttpStatus.OK.value()));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Code de vérification invalide.", HttpStatus.BAD_REQUEST.value()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Une erreur s'est produite lors de la vérification du code.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerificationCode(Authentication authentication) throws TooManyAttemptsException {
        return sendVerificationCode(authentication);
    }
}