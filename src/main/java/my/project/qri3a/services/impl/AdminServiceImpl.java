package my.project.qri3a.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UserUpdateRequestDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.repositories.search.ProductDocRepository;
import my.project.qri3a.services.AdminService;
import my.project.qri3a.services.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductDocRepository productDocRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final S3Service s3Service;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Arrays.stream(User.class.getDeclaredFields())
            .map(Field::getName)
            .collect(Collectors.toSet());

    /**
     * Vérifie si l'utilisateur connecté essaie d'agir sur son propre compte
     *
     * @param targetUserId L'ID de l'utilisateur cible
     * @return true si c'est le même utilisateur, false sinon
     */
    private boolean isSelfAction(UUID targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = userRepository.findByEmail(authentication.getName())
                    .orElse(null);

            if (currentUser != null && currentUser.getId().equals(targetUserId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Page<User> getPaginatedUsers(Pageable pageable) throws ResourceNotValidException {
        log.info("Admin Service: Fetching all users with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        validateSortParameters(pageable);
        return userRepository.findAll(pageable);
    }

    @Override
    public User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException {
        log.info("Admin Service: Creating user with email: {}", user.getEmail());

        // Vérifier si un utilisateur avec le même email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.error("User with email {} already exists", user.getEmail());
            throw new ResourceAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Enregistrer l'utilisateur
        User createdUser = userRepository.save(user);
        log.info("Admin Service: User created with ID: {}", createdUser.getId());
        return createdUser;
    }

    @Override
    public User updateUser(UUID userID, UserUpdateRequestDTO userRequestDTO)
            throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Admin Service: Updating user with ID: {}", userID);

        User userToUpdate = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Admin Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        // Mettre à jour les champs de l'utilisateur
        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().equals(userToUpdate.getEmail())) {
            // Vérifier si le nouvel email est déjà utilisé
            userRepository.findByEmail(userRequestDTO.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(userID)) {
                            throw new ResourceAlreadyExistsException("Email " + userRequestDTO.getEmail() + " is already in use");
                        }
                    });
            userToUpdate.setEmail(userRequestDTO.getEmail());
        }

        if (userRequestDTO.getName() != null) {
            userToUpdate.setName(userRequestDTO.getName());
        }

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isEmpty()) {
            userToUpdate.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        if (userRequestDTO.getPhoneNumber() != null) {
            userToUpdate.setPhoneNumber(userRequestDTO.getPhoneNumber());
        }

        if (userRequestDTO.getCity() != null) {
            userToUpdate.setCity(userRequestDTO.getCity());
        }

        if (userRequestDTO.getRole() != null) {
            userToUpdate.setRole(userRequestDTO.getRole());
        }

        if (userRequestDTO.getWebsite() != null) {
            userToUpdate.setWebsite(userRequestDTO.getWebsite());
        }

        if (userRequestDTO.getAboutMe() != null) {
            userToUpdate.setAboutMe(userRequestDTO.getAboutMe());
        }

        if (userRequestDTO.getProfileImage() != null) {
            userToUpdate.setProfileImage(userRequestDTO.getProfileImage());
        }

        User updatedUser = userRepository.save(userToUpdate);
        log.info("Admin Service: User updated with ID: {}", updatedUser.getId());
        return updatedUser;
    }

    @Override
    @Transactional
    public void deleteUser(UUID userID) throws ResourceNotFoundException, AccessDeniedException {
        log.info("Admin Service: Deleting user with ID: {}", userID);

        // Vérifier si l'admin essaie de se supprimer lui-même
        if (isSelfAction(userID)) {
            throw new AccessDeniedException("Administrateurs ne peuvent pas supprimer leur propre compte");
        }

        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Admin Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        List<UUID> productIds = productRepository.findProductIdsBySellerId(userID);

        // 1. Collecter les URLs des images à supprimer de S3 avant la suppression
        List<String> s3ImageUrls = entityManager.createQuery(
                        "SELECT i.url FROM Image i JOIN i.product p WHERE p.seller.id = :userId", String.class)
                .setParameter("userId", userID)
                .getResultList();

        // 2. Détacher l'utilisateur de la session pour éviter les problèmes de collections
        entityManager.detach(user);

        // 3. Supprimer manuellement tous les enregistrements liés dans le bon ordre
        // a. Supprimer les entrées de wishlist
        userRepository.deleteWishlistEntriesForSellerProducts(userID);
        userRepository.deleteAllUserWishlistEntries(userID);

        // b. Supprimer les notifications
        entityManager.createQuery("DELETE FROM Notification n WHERE n.user.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // c. Supprimer les préférences de notification
        entityManager.createQuery("DELETE FROM NotificationPreference np WHERE np.user.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // d. Supprimer les rapports
        entityManager.createQuery("DELETE FROM Report r WHERE r.reporter.id = :userId OR r.reportedUser.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // e. Supprimer les reviews
        entityManager.createQuery("DELETE FROM Review r WHERE r.user.id = :userId OR r.reviewer.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // f. Supprimer les images des produits
        entityManager.createQuery("DELETE FROM Image i WHERE i.product.seller.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // g. Supprimer les produits
        entityManager.createQuery("DELETE FROM Product p WHERE p.seller.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // h. Supprimer les codes de vérification
        entityManager.createQuery("DELETE FROM VerificationCode vc WHERE vc.user.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // i. Supprimer les tokens de réinitialisation de mot de passe
        entityManager.createQuery("DELETE FROM PasswordResetToken prt WHERE prt.user.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        // j. Finalement, supprimer l'utilisateur
        entityManager.createQuery("DELETE FROM User u WHERE u.id = :userId")
                .setParameter("userId", userID)
                .executeUpdate();

        log.info("Admin Service: Deleting product indices with IDs: {}", productIds);

        // Supprimer les documents Elasticsearch
        productDocRepository.deleteByIdIn(productIds);

        log.info("Admin Service: User deleted with ID: {}", userID);

        // 4. Supprimer les fichiers S3
        for (String imageUrl : s3ImageUrls) {
            try {
                String filename = extractFileName(imageUrl);
                s3Service.deleteFile(filename);
                log.info("Admin Service: Deleted image from S3 with filename: {}", filename);
            } catch (Exception e) {
                log.error("Failed to delete S3 file: {}", imageUrl, e);
            }
        }
    }

    @Override
    public User blockUser(UUID userID, boolean blocked) throws ResourceNotFoundException, AccessDeniedException {
        log.info("Admin Service: {} user with ID: {}", blocked ? "Blocking" : "Unblocking", userID);

        // Vérifier si l'admin essaie de se bloquer lui-même
        if (isSelfAction(userID)) {
            throw new AccessDeniedException("Administrateurs ne peuvent pas bloquer leur propre compte");
        }

        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Admin Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        // Mettre à jour l'état de blocage de l'utilisateur
        user.setBlocked(blocked);

        User updatedUser = userRepository.save(user);
        log.info("Admin Service: User {} with ID: {}", blocked ? "blocked" : "unblocked", userID);

        return updatedUser;
    }

    private void validateSortParameters(Pageable pageable) throws ResourceNotValidException {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
            log.info("Admin Service: No sort parameter provided. Defaulting to sort by createdAt descending.");
        }
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
                    throw new ResourceNotValidException("Invalid sort property: " + property);
                }
            });
        }
    }



    @Override
    public User resetPassword(UUID userID, String newPassword) throws ResourceNotFoundException {
        log.info("Admin Service: Resetting password for user with ID: {}", userID);

        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Admin Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        // Encoder et mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);

        log.info("Admin Service: Password reset successfully for user with ID: {}", userID);
        return updatedUser;
    }

    @Override
    public Page<User> searchUsers(
            String name,
            String email,
            String phone,
            String role,
            Boolean status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable) throws ResourceNotValidException {

        log.info("Admin Service: Searching users with filters - name: {}, email: {}, phone: {}, role: {}, status: {}, dateFrom: {}, dateTo: {}",
                name, email, phone, role, status, dateFrom, dateTo);

        validateSortParameters(pageable);

        Specification<User> spec = Specification.where(null);

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name").as(String.class)), "%" + name.toLowerCase() + "%"));
        }

        if (email != null && !email.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email").as(String.class)), "%" + email.toLowerCase() + "%"));
        }

        if (phone != null && !phone.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + phone + "%"));
        }

        if (role != null && !role.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), role));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("blocked"), status));
        }

        if (dateFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
        }

        if (dateTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
        }

        return userRepository.findAll(spec, pageable);
    }

    private String extractFileName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }
}