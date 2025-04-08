package my.project.qri3a.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ContactRequestDTO;
import my.project.qri3a.dtos.responses.ContactResponseDTO;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur dédié aux APIs publiques de contact
 * Ce contrôleur expose les endpoints accessibles depuis le frontend sans authentification
 */
@RestController
@RequestMapping("/api/v1/public/contact")
@RequiredArgsConstructor
@Slf4j
public class PublicContactController {

    private final ContactService contactService;

    /**
     * POST /api/v1/public/contact
     * Endpoint pour soumettre un formulaire de contact depuis le frontend
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponseDTO>> submitContact(
            @Valid @RequestBody ContactRequestDTO contactRequestDTO,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("Controller: Submitting public contact form from: {}", contactRequestDTO.getEmail());

        ContactResponseDTO responseDTO = contactService.submitContact(contactRequestDTO, request, authentication);

        ApiResponse<ContactResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "Votre message a été envoyé avec succès. Nous vous répondrons dans les plus brefs délais.",
                HttpStatus.CREATED.value()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}