package my.project.qri3a.services.impl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ChangePasswordRequestDTO;
import my.project.qri3a.dtos.requests.UserSettingsInfosDTO;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.SellerProfileDTO;
import my.project.qri3a.entities.Image;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ImageMapper;
import my.project.qri3a.mappers.ProductMapper;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.ImageRepository;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.repositories.VerificationCodeRepository;
import my.project.qri3a.services.ImageService;
import my.project.qri3a.services.S3Service;
import my.project.qri3a.services.UserService;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Arrays.stream(User.class.getDeclaredFields())
            .map(Field::getName)
            .collect(Collectors.toSet());
    private final S3Service s3Service;
    private final ImageService imageService;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final VerificationCodeRepository verificationCodeRepository;

    @Override
    public Page<User> getAllUsers(Pageable pageable) throws ResourceNotValidException {
        log.info("Service: Fetching all users with pagination: page={}, size={}, sort={}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        validateSortParameters(pageable);
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<User> getUserById(UUID userID) throws ResourceNotFoundException {
        log.info("Service: Fetching user with ID: {}", userID);
        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });
        return Optional.ofNullable(user);
    }

    @Override
    public User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException {
        log.info("Service: Creating user with email: {}", user.getEmail());

        // Vérifier si un utilisateur avec le même email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.error("User with email {} already exists", user.getEmail());
            throw new ResourceAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Enregistrer l'utilisateur
        User createdUser = userRepository.save(user);
        log.info("Service: User created with ID: {}", createdUser.getId());
        return createdUser;
    }

    @Override
    public User updateUser(UserSettingsInfosDTO dto, Authentication authentication)
            throws ResourceNotFoundException, BadCredentialsException, IOException, ResourceNotValidException {

        // Vérifier si l'utilisateur authentifié existe
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        User userToUpdate = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + currentUser.getId()));

        userToUpdate.setEmail(dto.getEmail());
        userToUpdate.setName(dto.getName());
        userToUpdate.setPhoneNumber(dto.getPhoneNumber());
        userToUpdate.setCity(dto.getCity());
        userToUpdate.setAboutMe(dto.getAboutMe());
        userToUpdate.setWebsite(dto.getWebsite());

        // Gestion de la photo de profil
        if (dto.getMultipartFile() != null && !dto.getMultipartFile().isEmpty()) {
            // Validation du type de fichier
            if (!isImageFile(dto.getMultipartFile())) {
                throw new ResourceNotValidException("Only image files are allowed for profile picture.");
            }

            // Si l'utilisateur a déjà une photo de profil, supprimer l'ancienne
            if (userToUpdate.getProfileImage() != null && !userToUpdate.getProfileImage().isEmpty()) {
                String filename = extractFileName(userToUpdate.getProfileImage());
                s3Service.deleteFile(filename);
                log.info("Old profile image deleted: {}", filename);
            }

            // Générer un nom unique pour la nouvelle photo
            String filename = generateUniqueFileName(dto.getMultipartFile().getOriginalFilename());

            // Télécharger la nouvelle photo sur S3
            String fileUrl = s3Service.uploadFile(dto.getMultipartFile(), filename);
            log.info("New profile image uploaded with URL: {}", fileUrl);

            // Mettre à jour l'URL de la photo de profil
            userToUpdate.setProfileImage(fileUrl);
        }
        // Nouveau cas: si removeProfileImage est true, supprimer l'image existante sans la remplacer
        else if (dto.isRemoveProfileImage()) {
            // Vérifier si l'utilisateur a une image de profil à supprimer
            if (userToUpdate.getProfileImage() != null && !userToUpdate.getProfileImage().isEmpty()) {
                String filename = extractFileName(userToUpdate.getProfileImage());
                s3Service.deleteFile(filename);
                log.info("Profile image deleted without replacement: {}", filename);

                // Réinitialiser l'URL de l'image de profil
                userToUpdate.setProfileImage(null);
            }
        }

        return userRepository.save(userToUpdate);
    }


    @Override
    public void deleteUser(UUID userID) throws ResourceNotFoundException {
        log.info("Service: Deleting user with ID: {}", userID);
        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });
        userRepository.delete(user);
        log.info("Service: User deleted with ID: {}", userID);
    }




    private void validateSortParameters(Pageable pageable) throws ResourceNotValidException {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
            log.info("Controller: No sort parameter provided. Defaulting to sort by createdAt descending.");
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
    public void addProductToWishlist(UUID userId, UUID productId) throws ResourceNotFoundException {
        log.info("Service: Adding product with ID {} to wishlist of user with ID {}", productId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("+ Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("+ Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        if (!user.getWishlist().contains(product)) {
            user.addToWishlist(product);
            userRepository.save(user);
            log.info("+ Service: Product with ID {} added to wishlist of user with ID {}", productId, userId);
        } else {
            log.info("+ Service: Product with ID {} is already in the wishlist of user with ID {}", productId, userId);
        }
    }


    @Override
    public void removeProductFromWishlist(UUID userId, UUID productId) throws ResourceNotFoundException {
        log.info("Service: Removing product with ID {} from wishlist of user with ID {}", productId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        user.removeFromWishlist(product);
        userRepository.save(user);
        log.info("Service: Product with ID {} removed from wishlist of user with ID {}", productId, userId);
    }

    /**
     * Supprime tous les produits de la wishlist de l'utilisateur.
     *
     * @param userId ID de l'utilisateur
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Override
    public void clearWishlist(UUID userId) throws ResourceNotFoundException {
        log.info("Service: Clearing wishlist for user with ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        user.clearWishlist();
        userRepository.save(user);
        log.info("Service: Wishlist cleared for user with ID {}", userId);
    }

    public Page<ProductListingDTO> getWishlist(UUID userId, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching wishlist for user with ID {}", userId);

        // Verify that the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        // Fetch paginated wishlist products directly from the repository
        Page<Product> wishlistPage = productRepository.findWishlistByUserId(userId, pageable);
        log.info("Service: User with ID {} has {} products in wishlist (Total: {})",
                userId, wishlistPage.getNumberOfElements(), wishlistPage.getTotalElements());

        // Map entities to DTOs
        return wishlistPage.map(productMapper::toProductListingDTO);
    }


    @Override
    public User getUserByEmail(String email) throws ResourceNotFoundException {
        log.info("Service: Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email " + email);
                });
    }

    @Override
    public List<UUID> getWishlistProductIds(UUID userId) throws ResourceNotFoundException {
        log.info("Service: Fetching wishlist product IDs for user with ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        return user.getWishlist().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
    }

    @Override
    public User getUserMe(Authentication authentication) throws ResourceNotFoundException {
        log.info("Service: Fetching current authenticated user");
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Service: Authenticated user not found with email: {}", email);
                    return new ResourceNotFoundException("Authenticated user not found");
                });
    }

    @Override
    @Transactional
    public void deleteUserMe(Authentication authentication) throws ResourceNotFoundException {
        log.info("Service: Deleting current authenticated user");
        User user = getUserMe(authentication);

        // Supprimer toutes les images des produits de l'utilisateur depuis S3 et la base de données
        for (Product product : user.getProducts()) {
            for (Image image : product.getImages()) {
                // Extraire le nom du fichier depuis l'URL
                String filename = extractFileName(image.getUrl());
                // Supprimer le fichier de S3
                s3Service.deleteFile(filename);
                log.info("Service: Deleted image from S3 with filename: {}", filename);
                // Supprimer l'image de la base de données
                imageService.deleteImageById(image.getId());
                log.info("Service: Deleted image from DB with ID: {}", image.getId());
            }
            // Supprimer le produit de la base de données
            productRepository.delete(product);
            log.info("Service: Deleted product with ID: {}", product.getId());
        }

        // Vider la wishlist de l'utilisateur
        user.getWishlist().clear();
        log.info("Service: Cleared wishlist for user ID: {}", user.getId());

        verificationCodeRepository.deleteByUserId(user.getId());
        // Supprimer l'utilisateur de la base de données
        userRepository.delete(user);
        log.info("Service: User deleted with ID: {}", user.getId());
    }

    private String extractFileName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }

    @Override
    public void changePassword(ChangePasswordRequestDTO changePasswordRequestDTO, Authentication authentication)
            throws ResourceNotFoundException, BadCredentialsException, ResourceNotValidException {
        log.info("Service: Changing password for user: {}", authentication.getName());

        // Récupérer l'utilisateur authentifié
        User user = getUserMe(authentication);

        // Vérifier si le mot de passe actuel est correct
        if (!passwordEncoder.matches(changePasswordRequestDTO.getCurrentPassword(), user.getPassword())) {
            log.warn("Service: Incorrect current password for user: {}", user.getEmail());
            throw new BadCredentialsException("Mot de passe actuel incorrect.");
        }

        // Vérifier que le nouveau mot de passe correspond à la confirmation
        if (!changePasswordRequestDTO.getNewPassword().equals(changePasswordRequestDTO.getConfirmPassword())) {
            log.warn("Service: New password and confirmation do not match for user: {}", user.getEmail());
            throw new ResourceNotValidException("Les nouveaux mots de passe ne correspondent pas.");
        }

        // Optionnel : Ajouter d'autres validations sur le nouveau mot de passe si nécessaire

        // Encoder et mettre à jour le mot de passe
        String encodedNewPassword = passwordEncoder.encode(changePasswordRequestDTO.getNewPassword());
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        log.info("Service: Password changed successfully for user: {}", user.getEmail());
    }

    @Override
    public SellerProfileDTO getSellerProfile(UUID userId) throws ResourceNotFoundException {
        log.info("Service: Fetching seller profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        // Map User entity to SellerProfileDTO
        SellerProfileDTO sellerProfileDTO = userMapper.toSellerProfileDTO(user);

        // Fetch top 3 images for the seller's products
        List<Image> images = imageRepository.findTop3ByProductSellerIdOrderByCreatedAtDesc(userId);

        // Map Image entities to ImageResponseDTOs
        List<ImageResponseDTO> imageDTOs = images.stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());

        // Set images in the DTO
        sellerProfileDTO.setImages(imageDTOs);


        long totalProducts = productRepository.countBySellerId(userId);
        sellerProfileDTO.setTotalProducts(totalProducts);

        log.info("Service: Seller profile fetched for user ID: {}", userId);
        return sellerProfileDTO;
    }

    // Ajouter ces méthodes utilitaires
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/png") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp")
        );
    }

    private String generateUniqueFileName(String originalFilename) {
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "profile-" + UUID.randomUUID().toString() + fileExtension;
    }



}
