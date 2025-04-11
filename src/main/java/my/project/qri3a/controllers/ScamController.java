package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
//import my.project.qri3a.dtos.requests.ScamUpdateRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
//import my.project.qri3a.dtos.responses.ScamStatisticsDTO;
//import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
//import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.services.ScamService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scams")
@RequiredArgsConstructor
@Slf4j
public class ScamController {

    private final ScamService scamService;

    /**
     * Endpoint pour créer un nouveau signalement d'arnaque.
     */
    @PostMapping
    public ResponseEntity<ScamResponseDTO> reportScam(
            @Valid @RequestBody ScamReportRequestDTO dto,
            Authentication authentication
    ) throws ResourceNotFoundException, ResourceAlreadyExistsException {
        log.info("Controller: Creating new scam report for product with ID: {}", dto.getProductId());
        ScamResponseDTO response = scamService.reportScam(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    /**
//     * Endpoint pour récupérer un signalement d'arnaque par son ID.
//     */
//    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or @scamAuthorizationService.canAccessScam(#id, authentication)")
//    public ResponseEntity<ScamResponseDTO> getScamById(
//            @PathVariable("id") UUID id
//    ) throws ResourceNotFoundException {
//        log.info("Controller: Fetching scam report with ID: {}", id);
//        ScamResponseDTO response = scamService.getScamById(id);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour mettre à jour le statut d'un signalement d'arnaque (admin).
//     */
//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ScamResponseDTO> updateScamStatus(
//            @PathVariable("id") UUID id,
//            @Valid @RequestBody ScamUpdateRequestDTO dto,
//            Authentication authentication
//    ) throws ResourceNotFoundException, ResourceNotValidException {
//        log.info("Controller: Updating scam report with ID: {}", id);
//        ScamResponseDTO response = scamService.updateScamStatus(id, dto, authentication);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour supprimer un signalement d'arnaque (admin).
//     */
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteScam(
//            @PathVariable("id") UUID id
//    ) throws ResourceNotFoundException {
//        log.info("Controller: Deleting scam report with ID: {}", id);
//        scamService.deleteScam(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    /**
//     * Endpoint pour récupérer tous les signalements d'arnaque (admin).
//     */
//    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Page<ScamResponseDTO>> getAllScams(
//            @PageableDefault(size = 10) Pageable pageable
//    ) {
//        log.info("Controller: Fetching all scam reports with pagination");
//        Page<ScamResponseDTO> response = scamService.getAllScams(pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour récupérer les signalements d'arnaque par statut (admin).
//     */
//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Page<ScamResponseDTO>> getScamsByStatus(
//            @PathVariable("status") ScamStatus status,
//            @PageableDefault(size = 10) Pageable pageable
//    ) {
//        log.info("Controller: Fetching scam reports with status: {}", status);
//        Page<ScamResponseDTO> response = scamService.getScamsByStatus(status, pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour récupérer les signalements d'arnaque effectués par l'utilisateur connecté.
//     */
//    @GetMapping("/my-reports")
//    public ResponseEntity<Page<ScamResponseDTO>> getMyScamReports(
//            Authentication authentication,
//            @PageableDefault(size = 10) Pageable pageable
//    ) throws ResourceNotFoundException {
//        log.info("Controller: Fetching scam reports for current user");
//        Page<ScamResponseDTO> response = scamService.getScamsByReporter(authentication, pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour récupérer les signalements d'arnaque concernant un produit (admin, vendeur).
//     */
//    @GetMapping("/product/{productId}")
//    @PreAuthorize("hasRole('ADMIN') or @productAuthorizationService.isProductOwner(#productId, authentication)")
//    public ResponseEntity<Page<ScamResponseDTO>> getScamsByProduct(
//            @PathVariable("productId") UUID productId,
//            @PageableDefault(size = 10) Pageable pageable
//    ) throws ResourceNotFoundException {
//        log.info("Controller: Fetching scam reports for product with ID: {}", productId);
//        Page<ScamResponseDTO> response = scamService.getScamsByProduct(productId, pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour récupérer les signalements d'arnaque concernant les produits d'un vendeur (admin, vendeur).
//     */
//    @GetMapping("/seller/{sellerId}")
//    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.getUserById(#sellerId).get().email")
//    public ResponseEntity<Page<ScamResponseDTO>> getScamsBySeller(
//            @PathVariable("sellerId") UUID sellerId,
//            @RequestParam(defaultValue = "PENDING") ScamStatus status,
//            @PageableDefault(size = 10) Pageable pageable
//    ) throws ResourceNotFoundException {
//        log.info("Controller: Fetching scam reports for seller with ID: {} and status: {}", sellerId, status);
//        Page<ScamResponseDTO> response = scamService.getScamsBySeller(sellerId, status, pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour obtenir des statistiques sur les signalements d'arnaque (admin).
//     */
//    @GetMapping("/statistics")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ScamStatisticsDTO> getScamStatistics() {
//        log.info("Controller: Fetching scam report statistics");
//        ScamStatisticsDTO response = scamService.getScamStatistics();
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint pour vérifier si un produit a des signalements confirmés.
//     */
//    @GetMapping("/product/{productId}/has-confirmed")
//    public ResponseEntity<Boolean> hasConfirmedScams(
//            @PathVariable("productId") UUID productId
//    ) {
//        log.info("Controller: Checking if product with ID {} has confirmed scams", productId);
//        boolean hasConfirmed = scamService.hasConfirmedScams(productId);
//        return ResponseEntity.ok(hasConfirmed);
//    }


}