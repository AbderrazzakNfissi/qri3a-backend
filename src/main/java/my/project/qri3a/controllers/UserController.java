package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * GET /api/v1/users?page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getAllUsers(Pageable pageable) throws ResourceNotValidException {
        log.info("Controller: Fetching all users with pagination: page={}, size={}, sort={}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<User> usersPage = userService.getAllUsers(pageable);
        Page<UserResponseDTO> dtoPage = usersPage.map(userMapper::toDTO);
        ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>(dtoPage, "Users fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable(value = "id") UUID userID) throws ResourceNotFoundException{
        log.info("Controller: Fetching user with ID: {}", userID);
        try {
            User user = userService.getUserById(userID)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userID));
            UserResponseDTO responseDTO = userMapper.toDTO(user);
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(responseDTO, "User fetched successfully.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error fetching user: {}", ex.getMessage());
            ApiResponse<UserResponseDTO> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /api/v1/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO)
            throws ResourceAlreadyExistsException, ResourceNotValidException{
        log.info("Controller: Creating new user with email: {}", userRequestDTO.getEmail());
        try {
            User user = userMapper.toEntity(userRequestDTO);
            User createdUser = userService.createUser(user);
            UserResponseDTO responseDTO = userMapper.toDTO(createdUser);
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(responseDTO, "User created successfully.", HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceAlreadyExistsException ex) {
            log.error("Error creating user: {}", ex.getMessage());
            ApiResponse<UserResponseDTO> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.CONFLICT.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        } catch (ResourceNotValidException ex) {
            log.error("Validation error creating user: {}", ex.getMessage());
            ApiResponse<UserResponseDTO> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.FORBIDDEN.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * PUT /api/v1/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable(value = "id") UUID userID,
                                                                   @Valid @RequestBody UserRequestDTO userRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Controller: Updating user with ID: {}", userID);

        User updatedUser = userService.updateUser(userID, userRequestDTO);
        UserResponseDTO responseDTO = userMapper.toDTO(updatedUser);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(responseDTO, "User updated successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable(value = "id") UUID userID)  throws ResourceNotFoundException{
        log.info("Controller: Deleting user with ID: {}", userID);
        try {
            userService.deleteUser(userID);
            ApiResponse<Void> response = new ApiResponse<>(null, "User deleted successfully.", HttpStatus.NO_CONTENT.value());
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            log.error("Error deleting user: {}", ex.getMessage());
            ApiResponse<Void> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /api/v1/users/{userId}/wishlist/{productId}
     * Ajouter un produit à la wishlist de l'utilisateur
     */
    @PostMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<ApiResponse<String>> addProductToWishlist(@PathVariable UUID userId, @PathVariable UUID productId) {
        log.info("Controller: Adding product with ID {} to wishlist of user with ID {}", productId, userId);
        try {
            userService.addProductToWishlist(userId, productId);
            ApiResponse<String> response = new ApiResponse<>("Product added to wishlist successfully.", "Product added to wishlist successfully.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error adding product to wishlist: {}", ex.getMessage());
            ApiResponse<String> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /api/v1/users/{userId}/wishlist/{productId}
     * Retirer un produit de la wishlist de l'utilisateur
     */
    @DeleteMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<ApiResponse<String>> removeProductFromWishlist(@PathVariable UUID userId, @PathVariable UUID productId) {
        log.info("Controller: Removing product with ID {} from wishlist of user with ID {}", productId, userId);
        try {
            userService.removeProductFromWishlist(userId, productId);
            ApiResponse<String> response = new ApiResponse<>("Product removed from wishlist successfully.", "Product removed from wishlist successfully.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error removing product from wishlist: {}", ex.getMessage());
            ApiResponse<String> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }


    /**
     * DELETE /api/v1/users/{userId}/wishlist
     * Supprimer tous les produits de la wishlist de l'utilisateur
     */
    @DeleteMapping("/{userId}/wishlist")
    public ResponseEntity<ApiResponse<String>> clearWishlist(@PathVariable UUID userId) {
        log.info("Controller: Clearing wishlist for user with ID {}", userId);
        try {
            userService.clearWishlist(userId);
            ApiResponse<String> response = new ApiResponse<>("Wishlist cleared successfully.", "All products removed from wishlist.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error clearing wishlist: {}", ex.getMessage());
            ApiResponse<String> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{userId}/wishlist")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getWishlist(@PathVariable UUID userId) {
        log.info("Controller: Fetching wishlist for user with ID {}", userId);
        try {
            List<ProductResponseDTO> wishlistProducts = userService.getWishlist(userId);
            ApiResponse<List<ProductResponseDTO>> response = new ApiResponse<>(wishlistProducts, "Wishlist fetched successfully.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error fetching wishlist: {}", ex.getMessage());
            ApiResponse<List<ProductResponseDTO>> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }


}
