package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UpdateUserRequestDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        /*
        if (authentication != null && authentication.isAuthenticated()) {
            String currentPrincipalName = authentication.getName();
            log.info("==> Controller: Fetching all users with current principal: {}", currentPrincipalName);
        }
        */
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

            User user = userService.getUserById(userID)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userID));
            UserResponseDTO responseDTO = userMapper.toDTO(user);
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(responseDTO, "User fetched successfully.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
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
                                                                   @Valid @RequestBody UpdateUserRequestDTO userRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
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
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable(value = "id") UUID userID) throws ResourceNotFoundException {
        log.info("Controller: Deleting user with ID: {}", userID);

        userService.deleteUser(userID);
        ApiResponse<Void> response = new ApiResponse<>(null, "User deleted successfully.", HttpStatus.NO_CONTENT.value());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }




}
