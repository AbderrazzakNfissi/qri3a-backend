package my.project.qri3a.controllers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamUpdateRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.dtos.responses.ScamStatisticsDTO;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.services.ScamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur dédié à la gestion des signalements d'arnaque par les administrateurs.
 * Ce contrôleur expose des endpoints avec le préfixe /api/v1/admin/scams.
 */
@RestController
@RequestMapping("/api/v1/admin/scams")
@RequiredArgsConstructor
@Slf4j
public class AdminScamController {

    private final ScamService scamService ;

    /**
     * Récupère tous les signalements d'arnaque avec pagination.
     */
    @GetMapping
    public ResponseEntity<Page<ScamResponseDTO>> getAllScams(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        log.info("Admin Controller: Fetching all scam reports with pagination");
        Page<ScamResponseDTO> response = scamService.getAllScams(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les signalements d'arnaque filtrés par statut.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ScamResponseDTO>> getScamsByStatus(
            @PathVariable("status") ScamStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        log.info("Admin Controller: Fetching scam reports with status: {}", status);
        Page<ScamResponseDTO> response = scamService.getScamsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour le statut d'un signalement d'arnaque.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScamResponseDTO> updateScamStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ScamUpdateRequestDTO dto,
            Authentication authentication
    ) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Admin Controller: Updating status for scam report with ID: {} to {}", id, dto.getStatus());
        ScamResponseDTO response = scamService.updateScamStatus(id, dto, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Supprime un signalement d'arnaque.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScam(
            @PathVariable("id") UUID id
    ) throws ResourceNotFoundException {
        log.info("Admin Controller: Deleting scam report with ID: {}", id);
        scamService.deleteScam(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les statistiques des signalements d'arnaque.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ScamStatisticsDTO> getScamStatistics() {
        log.info("Admin Controller: Fetching scam report statistics");
        ScamStatisticsDTO response = scamService.getScamStatistics();
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les signalements d'arnaque pour un produit spécifique par son identifiant.
     */
    @GetMapping("/product/{productIdentifier}")
    public ResponseEntity<Page<ScamResponseDTO>> getScamsByProductIdentifier(
            @PathVariable("productIdentifier") String productIdentifier,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) throws ResourceNotFoundException {
        log.info("Admin Controller: Fetching scam reports for product with identifier: {}", productIdentifier);
        Page<ScamResponseDTO> response = scamService.getScamsByProductIdentifier(productIdentifier, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Traite en masse les signalements d'arnaque en attente pour un produit spécifique.
     */
    @PutMapping("/product/{productIdentifier}/bulk-update")
    public ResponseEntity<Void> bulkUpdateScamsForProduct(
            @PathVariable("productIdentifier") String productIdentifier,
            @Valid @RequestBody ScamUpdateRequestDTO dto,
            Authentication authentication,
            @PageableDefault(size = 100) Pageable pageable
    ) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Admin Controller: Bulk updating scam reports for product with identifier: {} to status: {}", 
            productIdentifier, dto.getStatus());

        // Récupérer tous les signalements pour ce produit
        Page<ScamResponseDTO> scams = scamService.getScamsByProductIdentifier(productIdentifier, pageable);

        // Mettre à jour chaque signalement
        for (ScamResponseDTO scam : scams.getContent()) {
            try {
                scamService.updateScamStatus(scam.getId(), dto, authentication);
            } catch (Exception e) {
                log.error("Failed to update scam with ID: {}", scam.getId(), e);
                // Continuer avec les autres signalements même si un échoue
            }
        }

        return ResponseEntity.noContent().build();
    }
}