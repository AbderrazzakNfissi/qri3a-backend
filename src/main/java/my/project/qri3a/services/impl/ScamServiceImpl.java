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
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
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
    public ScamResponseDTO reportScam(ScamReportRequestDTO dto, Authentication authentication)
            throws ResourceNotFoundException, ResourceAlreadyExistsException {
        log.info("Service: Creating scam report for product with ID: {}", dto.getProductId());

        // Récupérer l'utilisateur authentifié
        User reporter = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Récupérer le produit concerné
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + dto.getProductId()));

        // Vérifier si l'utilisateur a déjà signalé ce produit
        if (scamRepository.existsByReporterIdAndReportedProductId(reporter.getId(), product.getId())) {
            throw new ResourceAlreadyExistsException("You have already reported this product");
        }

        // Créer et sauvegarder le signalement
        Scam scam = scamMapper.toEntity(dto, reporter, product);
        Scam savedScam = scamRepository.save(scam);

        // Envoyer une notification à l'administrateur
        //notifyAdminsAboutNewScam(savedScam);

        log.info("Service: Scam report created with ID: {}", savedScam.getId());
        return scamMapper.toDTO(savedScam);
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

        // Si le signalement est confirmé, mettre à jour le statut du produit
        if (dto.getStatus() == ScamStatus.CONFIRMED) {
            handleConfirmedScam(scam);
        }

        // Sauvegarder les modifications
        Scam updatedScam = scamRepository.save(scam);

        // Notifier le reporter et le vendeur du produit
        //notifyUsersAboutScamStatusUpdate(updatedScam);

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
    public Page<ScamResponseDTO> getScamsByReporter(Authentication authentication, Pageable pageable)
            throws ResourceNotFoundException {
        log.info("Service: Fetching scam reports by current user");

        User reporter = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Scam> scams = scamRepository.findByReporterId(reporter.getId(), pageable);
        return scams.map(scamMapper::toDTO);
    }

    @Override
    public Page<ScamResponseDTO> getScamsByProduct(UUID productId, Pageable pageable)
            throws ResourceNotFoundException {
        log.info("Service: Fetching scam reports for product with ID: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID " + productId);
        }

        Page<Scam> scams = scamRepository.findByReportedProductId(productId, pageable);
        return scams.map(scamMapper::toDTO);
    }

    @Override
    public Page<ScamResponseDTO> getScamsBySeller(UUID sellerId, ScamStatus status, Pageable pageable)
            throws ResourceNotFoundException {
        log.info("Service: Fetching scam reports for seller with ID: {} and status: {}", sellerId, status);

        if (!userRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller not found with ID " + sellerId);
        }

        Page<Scam> scams = scamRepository.findByStatusAndReportedProductSellerId(status, sellerId, pageable);
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
                .uniqueProductsWithScams(scamRepository.countUniqueProductsWithScamsByStatus(ScamStatus.CONFIRMED))
                .productsWithMultipleScams(scamRepository.findProductsWithScamCountGreaterThan(2).size())
                .build();
    }

    @Override
    public boolean hasConfirmedScams(UUID productId) {
        log.info("Service: Checking if product with ID {} has confirmed scams", productId);
        return scamRepository.existsByReportedProductIdAndStatus(productId, ScamStatus.CONFIRMED);
    }

    // Méthodes privées utilitaires

    private void handleConfirmedScam(Scam scam) {
        Product product = scam.getReportedProduct();

        // Marquer le produit comme BLOCKED
        product.setStatus(ProductStatus.REJECTED);
//        product.setBlockReason("Signalement d'arnaque confirmé par un administrateur");
//        product.setBlockedAt(LocalDateTime.now());
//        product.setBlockedBy(scam.getProcessedBy());

        productRepository.save(product);

        log.info("Service: Product with ID {} has been blocked due to confirmed scam", product.getId());
    }



    private void notifyUsersAboutScamStatusUpdate(Scam scam) {
        try {
            // Créer l'objet Notification pour le reporter
            Notification reporterNotification = new Notification();
            reporterNotification.setUser(scam.getReporter());

            reporterNotification.setBody("Le statut de votre signalement d'arnaque pour le produit \""
                    + scam.getReportedProduct().getTitle() + "\" a été mis à jour en "
                    + scam.getStatus().getLabel() + ".");
            //reporterNotification.setLink("/my-scams/" + scam.getId());
            reporterNotification.setRead(false);
            // Autres propriétés si nécessaire

            // Notifier le reporter
            notificationService.createNotification(reporterNotification);

            // Créer l'objet Notification pour le vendeur
            Notification sellerNotification = new Notification();
            sellerNotification.setUser(scam.getReportedProduct().getSeller());
            sellerNotification.setBody("Un signalement d'arnaque concernant votre produit \""
                    + scam.getReportedProduct().getTitle() + "\" a été "
                    + (scam.getStatus() == ScamStatus.CONFIRMED ? "confirmé" : "traité") + ".");
            //sellerNotification.setLink("/my-products/scams/" + scam.getId());
            sellerNotification.setRead(false);
            // Autres propriétés si nécessaire

            // Notifier le vendeur
            notificationService.createNotification(sellerNotification);

        } catch (Exception e) {
            log.error("Failed to send notifications about scam status update", e);
        }
    }

    @Override
    public long countScamsByStatus(ScamStatus status) {
        log.info("Service: Counting scam reports with status: {}", status);
        return scamRepository.countByStatus(status);
    }
}