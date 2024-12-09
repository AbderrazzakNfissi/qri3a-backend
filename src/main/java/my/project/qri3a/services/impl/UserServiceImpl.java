package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
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
    public User updateUser(UUID userID, UserRequestDTO userRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Updating user with ID: {}", userID);

        User user = userRepository.findById(userID)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userID);
                    return new ResourceNotFoundException("User not found with ID " + userID);
                });

        // Vérifier si l'email est mis à jour et s'il est unique
        if (!user.getEmail().equals(userRequestDTO.getEmail())) {
            if (userRepository.findByEmail(userRequestDTO.getEmail()).isPresent()) {
                log.error("User with email {} already exists", userRequestDTO.getEmail());
                throw new ResourceAlreadyExistsException("User with email " + userRequestDTO.getEmail() + " already exists");
            }
        }

        // Mettre à jour les champs de l'utilisateur à partir du DTO, en excluant 'password' et 'newPassword'
        userMapper.updateEntityFromDTO(userRequestDTO, user);

        // Vérifier et mettre à jour le mot de passe si 'newPassword' est fourni
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

    // Implémentation des nouvelles méthodes pour gérer les relations

    @Override
    public void addProductToUser(UUID userId, UUID productId) throws ResourceNotFoundException {
        log.info("Service: Adding product with ID {} to user with ID {}", productId, userId);
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

        user.addProduct(product);
        userRepository.save(user);
        log.info("Service: Product with ID {} added to user with ID {}", productId, userId);
    }

    @Override
    public void removeProductFromUser(UUID userId, UUID productId) throws ResourceNotFoundException {
        log.info("Service: Removing product with ID {} from user with ID {}", productId, userId);
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

        user.removeProduct(product);
        userRepository.save(user);
        log.info("Service: Product with ID {} removed from user with ID {}", productId, userId);
    }

    @Override
    public void addProductToWishlist(UUID userId, UUID productId) throws ResourceNotFoundException {
        log.info("Service: Adding product with ID {} to wishlist of user with ID {}", productId, userId);
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

        user.addToWishlist(product);
        userRepository.save(user);
        log.info("Service: Product with ID {} added to wishlist of user with ID {}", productId, userId);
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
}
