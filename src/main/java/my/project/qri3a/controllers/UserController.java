package my.project.qri3a.controllers;

import java.io.IOException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ChangePasswordRequestDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.requests.UserSettingsInfosDTO;
import my.project.qri3a.dtos.responses.SellerProfileDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.UserService;
import org.springframework.web.multipart.MultipartFile;

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
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSettingsInfosDTO>> getUserMe(Authentication authentication) throws ResourceNotFoundException {
        log.info("Controller: Fetching current authenticated user");

        User user = userService.getUserMe(authentication);
        UserSettingsInfosDTO responseDTO = userMapper.toUserSettingsInfosDTO(user);
        ApiResponse<UserSettingsInfosDTO> response = new ApiResponse<>(
                responseDTO,
                "Current user fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    /**
     * DELETE /api/v1/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUserMe(Authentication authentication) throws ResourceNotFoundException {
        log.info("Controller: Deleting current authenticated user");

        userService.deleteUserMe(authentication);
        ApiResponse<Void> response = new ApiResponse<>(null, "User deleted successfully.", HttpStatus.NO_CONTENT.value());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordRequestDTO,
            Authentication authentication) throws ResourceNotFoundException, BadCredentialsException, ResourceNotValidException {
        log.info("Controller: Changing password for user: {}", authentication.getName());

        userService.changePassword(changePasswordRequestDTO, authentication);

        ApiResponse<Void> response = new ApiResponse<>(
                null,
                "Mot de passe changé avec succès.",
                HttpStatus.OK.value()
        );
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
    @PutMapping("/update/personal-info")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserInfo(
            @Valid @RequestPart("userData") UserSettingsInfosDTO userSettingsInfosDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication)
            throws ResourceNotFoundException, ResourceNotValidException, IOException {

        // Associer l'image de profil au DTO
        if (profileImage != null && !profileImage.isEmpty()) {
            userSettingsInfosDTO.setMultipartFile(profileImage);
        }

        User updatedUser = userService.updateUser(userSettingsInfosDTO, authentication);
        UserResponseDTO responseDTO = userMapper.toDTO(updatedUser);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "User updated successfully.",
                HttpStatus.OK.value()
        );
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


    @GetMapping("/seller-profile/{id}")
    public ResponseEntity<ApiResponse<SellerProfileDTO>> getSellerProfile(@PathVariable("id") UUID userId) throws ResourceNotFoundException {
        log.info("Controller: Fetching seller profile for user ID: {}", userId);

        SellerProfileDTO sellerProfile = userService.getSellerProfile(userId);
        ApiResponse<SellerProfileDTO> response = new ApiResponse<>(
                sellerProfile,
                "Seller profile fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }



}
