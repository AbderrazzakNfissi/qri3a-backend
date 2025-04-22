package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.BlockUserRequestDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.requests.UserUpdateRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.AdminService;
import my.project.qri3a.services.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;
    private final ProductService productService;

    /**
     * GET /api/v1/admin/users?page=0&size=10&sort=name,asc
     * Récupère une liste paginée de tous les utilisateurs (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getPaginatedUsers(Pageable pageable)
            throws ResourceNotValidException {
        log.info("Admin Controller: Fetching all users with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<User> usersPage = adminService.getPaginatedUsers(pageable);
        Page<UserResponseDTO> dtoPage = usersPage.map(userMapper::toDTO);

        ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>(
                dtoPage,
                "Users fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/users/search
     * Recherche avancée d'utilisateurs (admin only)
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Pageable pageable) throws ResourceNotValidException {

        log.info("Admin Controller: Searching users with filters - name: {}, email: {}, phone: {}, role: {}, status: {}, dateFrom: {}, dateTo: {}",
                name, email, phone, role, status, dateFrom, dateTo);

        // Convert string parameters to appropriate types
        Boolean statusBool = status != null ? "blocked".equalsIgnoreCase(status) : null;

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        try {
            if (dateFrom != null && !dateFrom.isEmpty()) {
                fromDate = LocalDateTime.parse(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                toDate = LocalDateTime.parse(dateTo);
            }
        } catch (Exception e) {
            throw new ResourceNotValidException("Invalid date format. Expected format is ISO-8601 (yyyy-MM-dd)");
        }

        Page<User> usersPage = adminService.searchUsers(
                name,
                email,
                phone,
                role,
                statusBool,
                fromDate,
                toDate,
                pageable);

        Page<UserResponseDTO> dtoPage = usersPage.map(userMapper::toDTO);

        ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>(
                dtoPage,
                "Users search completed successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/admin/users
     * Crée un nouvel utilisateur (admin only)
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO)
            throws ResourceAlreadyExistsException, ResourceNotValidException {
        log.info("Admin Controller: Creating new user with email: {}", userRequestDTO.getEmail());

        User user = userMapper.toEntity(userRequestDTO);
        User createdUser = adminService.createUser(user);
        UserResponseDTO responseDTO = userMapper.toDTO(createdUser);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "User created successfully by admin.",
                HttpStatus.CREATED.value()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserResponseDTO>> resetUserPassword(
            @PathVariable(value = "id") UUID userID,
            @RequestBody Map<String, String> passwordRequest) throws ResourceNotFoundException {

        log.info("Admin Controller: Resetting password for user with ID: {}", userID);

        String newPassword = passwordRequest.get("password");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ResourceNotValidException("Password cannot be empty");
        }

        User updatedUser = adminService.resetPassword(userID, newPassword);
        UserResponseDTO responseDTO = userMapper.toDTO(updatedUser);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "User password reset successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/admin/users/{id}
     * Met à jour un utilisateur existant (admin only)
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(
            @PathVariable(value = "id") UUID userID,
            @Valid @RequestBody UserUpdateRequestDTO userRequestDTO)
            throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Admin Controller: Updating user with ID: {}", userID);

        User updatedUser = adminService.updateUser(userID, userRequestDTO);
        UserResponseDTO responseDTO = userMapper.toDTO(updatedUser);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "User updated successfully by admin.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/admin/users/{id}
     * Supprime un utilisateur (admin only)
     * Un admin ne peut pas supprimer son propre compte
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable(value = "id") UUID userID)
            throws ResourceNotFoundException {
        log.info("Admin Controller: Deleting user with ID: {}", userID);

        try {
            adminService.deleteUser(userID);

            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    "User deleted successfully by admin.",
                    HttpStatus.NO_CONTENT.value()
            );
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
        } catch (AccessDeniedException e) {
            log.error("Admin Controller: Access denied when trying to delete user with ID: {}", userID);
            ApiResponse<Void> response = new ApiResponse<>(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN.value()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    /**
     * PUT /api/v1/admin/users/{id}/block
     * Bloque ou débloque un utilisateur (admin only)
     * Un admin ne peut pas se bloquer lui-même
     */
    @PutMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse<UserResponseDTO>> blockUser(
            @PathVariable(value = "id") UUID userID,
            @Valid @RequestBody BlockUserRequestDTO blockUserRequestDTO)
            throws ResourceNotFoundException {
        log.info("Admin Controller: {} user with ID: {}",
                blockUserRequestDTO.isBlocked() ? "Blocking" : "Unblocking", userID);

        try {
            User updatedUser = adminService.blockUser(userID, blockUserRequestDTO.isBlocked());
            UserResponseDTO responseDTO = userMapper.toDTO(updatedUser);

            String message = blockUserRequestDTO.isBlocked()
                    ? "User blocked successfully."
                    : "User unblocked successfully.";

            ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                    responseDTO,
                    message,
                    HttpStatus.OK.value()
            );
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            log.error("Admin Controller: Access denied when trying to block/unblock user with ID: {}", userID);
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN.value()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @PostMapping("/products/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> approveProduct(@PathVariable UUID id)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Approving product with ID: {}", id);
        ProductResponseDTO approvedProduct = productService.approveProduct(id);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                approvedProduct,
                "Product approved successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> rejectProduct(@PathVariable UUID id)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Rejecting product with ID: {}", id);
        ProductResponseDTO rejectedProduct = productService.rejectProduct(id);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                rejectedProduct,
                "Product rejected successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    /**
     * GET /api/v1/admin/products/moderation
     * Liste tous les produits en attente de modération (admin only)
     */
    @GetMapping("/products/moderation")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getProductsInModeration(Pageable pageable) {
        log.info("Admin Controller: Fetching products in moderation with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getModerationProducts(pageable);

        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Products in moderation fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/products/pending-count
     * Obtient le nombre de produits en attente de modération
     */
    @GetMapping("/products/pending-count")
    public ResponseEntity<ApiResponse<Long>> getPendingProductsCount() {
        log.info("Admin Controller: Getting count of products pending moderation");

        long pendingCount = productService.getProductCountByStatus(ProductStatus.MODERATION);

        ApiResponse<Long> response = new ApiResponse<>(
                pendingCount,
                "Pending products count fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/products/status-counts
     * Obtient le nombre de produits par statut
     */
    @GetMapping("/products/status-counts")
    public ResponseEntity<ApiResponse<Map<ProductStatus, Long>>> getProductStatusCounts() {
        log.info("Admin Controller: Getting product counts by status");

        Map<ProductStatus, Long> statusCounts = productService.getAllProductCounts();

        ApiResponse<Map<ProductStatus, Long>> response = new ApiResponse<>(
                statusCounts,
                "Product status counts fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/admin/products/batch-approve
     * Approuver en masse une liste de produits en attente de modération
     */
    @PostMapping("/products/batch-approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchApproveProducts(
            @RequestBody List<UUID> productIds) {
        log.info("Admin Controller: Batch approving {} products", productIds.size());
        
        int approvedCount = productService.approveBatchProducts(productIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRequested", productIds.size());
        result.put("approvedCount", approvedCount);
        
        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                result,
                String.format("%d products approved successfully out of %d requested.", approvedCount, productIds.size()),
                HttpStatus.OK.value()
        );
        
        return ResponseEntity.ok(response);
    }
}