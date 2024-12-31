package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UpdateUserRequestDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ProductMapper;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.lang.reflect.Field;
import java.nio.CharBuffer;
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
    public User updateUser(UUID userID, UpdateUserRequestDTO userRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Updating user with ID: {}", userID);

        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("-> Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        if (!user.getEmail().equals(userRequestDTO.getEmail())) {
            if (userRepository.findByEmail(userRequestDTO.getEmail()).isPresent()) {
                log.warn("User with email {} already exists", userRequestDTO.getEmail());
                throw new ResourceAlreadyExistsException("User with email " + userRequestDTO.getEmail() + " already exists");
            }
        }

        userMapper.updateEntityFromDTO(userRequestDTO, user);

        if (userRequestDTO.getNewPassword() != null && !userRequestDTO.getNewPassword().isEmpty()) {
            // Vérifier que 'password' (ancien mot de passe) est fourni

            if (userRequestDTO.getPassword() == null || userRequestDTO.getPassword().isEmpty()) {
                log.error("Service: Current password is required to set a new password for user ID: {}", userID);
                throw new ResourceNotValidException("Current password is required to set a new password.");
            }


            // Vérifier que le 'password' fourni correspond au mot de passe stocké
            if (!passwordEncoder.matches(userRequestDTO.getPassword(), user.getPassword())) {
                log.error("Service: Current password is incorrect for user ID: {}", userID);
                throw new ResourceNotValidException("Current password is incorrect.");
            }


            // Mettre à jour le mot de passe (encodé)
            user.setPassword(passwordEncoder.encode(userRequestDTO.getNewPassword()));
            log.info("Service: Password updated for user ID: {}", userID);
        }

        // Enregistrer l'utilisateur mis à jour
        User updatedUser = userRepository.save(user);
        log.info("Service: User updated with ID: {}", updatedUser.getId());
        return updatedUser;
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

    @Override
    public List<ProductResponseDTO> getWishlist(UUID userId) throws ResourceNotFoundException {
        log.info("Service: Fetching wishlist for user with ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        List<Product> wishlistProducts = user.getWishlist();
        log.info("Service: User with ID {} has {} products in wishlist", userId, wishlistProducts.size());

        return wishlistProducts.stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
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
}
