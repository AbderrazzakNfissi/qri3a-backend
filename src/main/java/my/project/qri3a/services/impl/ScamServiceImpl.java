package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
import my.project.qri3a.dtos.requests.ScamUpdateRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.dtos.responses.ScamStatisticsDTO;
import my.project.qri3a.entities.Notification;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.Scam;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.enums.Role;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ScamMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.ScamRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.NotificationService;
import my.project.qri3a.services.ScamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ScamServiceImpl implements ScamService {

    private final ScamRepository scamRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ScamMapper scamMapper;
    private final NotificationService notificationService;

    @Override
    public ScamResponseDTO reportScam(ScamReportRequestDTO dto)
            throws ResourceNotFoundException {
        log.info("Service: Creating anonymous scam report for product with ID: {}", dto.getProductIdentifier());

        // Créer et sauvegarder le signalement anonyme
        Scam scam = scamMapper.toEntity(dto);
        Scam savedScam = scamRepository.save(scam);

        // Envoyer une notification à l'administrateur
        //notifyAdminsAboutNewScam(savedScam);

        log.info("Service: Anonymous scam report created with ID: {}", savedScam.getId());
        return scamMapper.toDTO(savedScam);
    }

    // Méthode privée pour notifier les administrateurs d'un nouveau signalement
    private void notifyAdminsAboutNewScam(Scam scam) {
        try {
            // Récupérer tous les administrateurs
            for (User admin : userRepository.findByRole(Role.ADMIN)) {
                // Créer l'objet Notification pour l'administrateur
                Notification notification = new Notification();
                notification.setUser(admin);

                notification.setRead(false);

                // Notifier l'administrateur
                notificationService.createNotification(notification);
            }
        } catch (Exception e) {
            log.error("Failed to send notifications about new scam report", e);
        }
    }

    @Override
    public ScamResponseDTO getScamById(UUID scamId) throws ResourceNotFoundException {
        log.info("Service: Fetching scam report with ID: {}", scamId);

        Scam scam = scamRepository.findById(scamId)
                .orElseThrow(() -> new ResourceNotFoundException("Scam report not found with ID " + scamId));

        return scamMapper.toDTO(scam);
    }

    @Override
    public ScamResponseDTO updateScamStatus(UUID scamId, ScamUpdateRequestDTO dto, Authentication authentication)
            throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Updating scam report status with ID: {}", scamId);

        // Récupérer l'administrateur authentifié
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Vérifier que l'utilisateur a le rôle ADMIN
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can update scam report status");
        }

        // Récupérer le signalement d'arnaque
        Scam scam = scamRepository.findById(scamId)
                .orElseThrow(() -> new ResourceNotFoundException("Scam report not found with ID " + scamId));

        // Mettre à jour le statut
        scam.setStatus(dto.getStatus());
        scam.setAdminComment(dto.getAdminComment());
        scam.setProcessedBy(admin);
        scam.setProcessedAt(LocalDateTime.now());

        // Si le signalement est confirmé, mettre à jour le statut du produit si possible
        if (dto.getStatus() == ScamStatus.CONFIRMED) {
            try {
                handleConfirmedScam(scam);
            } catch (Exception e) {
                log.error("Error updating product status for confirmed scam: {}", e.getMessage());
            }
        }

        // Sauvegarder les modifications
        Scam updatedScam = scamRepository.save(scam);

        log.info("Service: Scam report status updated with ID: {}", updatedScam.getId());
        return scamMapper.toDTO(updatedScam);
    }

    @Override
    public void deleteScam(UUID scamId) throws ResourceNotFoundException {
        log.info("Service: Deleting scam report with ID: {}", scamId);

        if (!scamRepository.existsById(scamId)) {
            throw new ResourceNotFoundException("Scam report not found with ID " + scamId);
        }

        scamRepository.deleteById(scamId);
        log.info("Service: Scam report deleted with ID: {}", scamId);
    }

    @Override
    public Page<ScamResponseDTO> getAllScams(Pageable pageable) {
        log.info("Service: Fetching all scam reports with pagination");

        Page<Scam> scams = scamRepository.findAll(pageable);
        return scams.map(scamMapper::toDTO);
    }

    @Override
    public Page<ScamResponseDTO> getScamsByStatus(ScamStatus status, Pageable pageable) {
        log.info("Service: Fetching scam reports with status: {}", status);

        Page<Scam> scams = scamRepository.findByStatus(status, pageable);
        return scams.map(scamMapper::toDTO);
    }

    @Override
    public Page<ScamResponseDTO> getScamsByProductIdentifier(String productIdentifier, Pageable pageable)
            throws ResourceNotFoundException {
        log.info("Service: Fetching scam reports for product with identifier: {}", productIdentifier);

        Page<Scam> scams = scamRepository.findByProductIdentifier(productIdentifier, pageable);
        return scams.map(scamMapper::toDTO);
    }

    @Override
    public ScamStatisticsDTO getScamStatistics() {
        log.info("Service: Fetching scam report statistics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime oneMonthAgo = now.minusMonths(1);

        return ScamStatisticsDTO.builder()
                .totalScams(scamRepository.count())
                .pendingScams(scamRepository.countByStatus(ScamStatus.PENDING))
                .confirmedScams(scamRepository.countByStatus(ScamStatus.CONFIRMED))
                .rejectedScams(scamRepository.countByStatus(ScamStatus.REJECTED))
                .underReviewScams(scamRepository.countByStatus(ScamStatus.UNDER_REVIEW))
                .scamsLastDay(scamRepository.countScamsCreatedSince(oneDayAgo))
                .scamsLastWeek(scamRepository.countScamsCreatedSince(oneWeekAgo))
                .scamsLastMonth(scamRepository.countScamsCreatedSince(oneMonthAgo))
                .build();
    }

    @Override
    public boolean hasConfirmedScams(String productIdentifier) {
        log.info("Service: Checking if product with identifier {} has confirmed scams", productIdentifier);
        return scamRepository.existsByProductIdentifierAndStatus(productIdentifier, ScamStatus.CONFIRMED);
    }

    // Méthodes privées utilitaires

    private void handleConfirmedScam(Scam scam) {
        // Version adaptée pour l'approche anonyme
        
        try {
            // L'identifiant du produit pourrait être un UUID stocké sous forme de chaîne
            // On tente d'abord de le traiter comme un UUID
            try {
                UUID productId = UUID.fromString(scam.getProductIdentifier());
                Product product = productRepository.findById(productId).orElse(null);
                
                if (product != null) {
                    // Marquer le produit comme REJECTED
                    product.setStatus(ProductStatus.REJECTED);
                    productRepository.save(product);
                    
                    log.info("Service: Product with ID {} has been blocked due to confirmed scam", product.getId());
                } else {
                    log.warn("Could not find product with identifier {} to mark as rejected", scam.getProductIdentifier());
                }
            } catch (IllegalArgumentException e) {
                // L'identifiant n'est pas un UUID valide - c'est peut-être un autre format d'identifiant externe
                log.warn("Product identifier is not a valid UUID: {}", scam.getProductIdentifier());
                // Si nécessaire, implémenter ici une logique pour traiter d'autres formats d'identifiants
            }
        } catch (Exception e) {
            log.error("Error while handling confirmed scam: {}", e.getMessage());
        }
    }

    @Override
    public long countScamsByStatus(ScamStatus status) {
        log.info("Service: Counting scam reports with status: {}", status);
        return scamRepository.countByStatus(status);
    }
}