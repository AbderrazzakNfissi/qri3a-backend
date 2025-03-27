package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.BlockUserRequestDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.requests.UserUpdateRequestDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;

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
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable(value = "id") UUID userID)
            throws ResourceNotFoundException {
        log.info("Admin Controller: Deleting user with ID: {}", userID);

        adminService.deleteUser(userID);

        ApiResponse<Void> response = new ApiResponse<>(
                null,
                "User deleted successfully by admin.",
                HttpStatus.NO_CONTENT.value()
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    /**
     * PUT /api/v1/admin/users/{id}/block
     * Bloque ou débloque un utilisateur (admin only)
     */
    @PutMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse<UserResponseDTO>> blockUser(
            @PathVariable(value = "id") UUID userID,
            @Valid @RequestBody BlockUserRequestDTO blockUserRequestDTO)
            throws ResourceNotFoundException {
        log.info("Admin Controller: {} user with ID: {}",
                blockUserRequestDTO.isBlocked() ? "Blocking" : "Unblocking", userID);

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
    }
}