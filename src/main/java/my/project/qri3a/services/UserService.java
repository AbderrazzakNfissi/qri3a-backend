package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.ChangePasswordRequestDTO;
import my.project.qri3a.dtos.requests.UpdateUserRequestDTO;
import my.project.qri3a.dtos.requests.UserSettingsInfosDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserService {
    Page<User> getAllUsers(Pageable pageable) throws ResourceNotValidException ;
    Optional<User> getUserById(UUID userID) throws ResourceNotFoundException;
    User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException;
    User updateUser(UserSettingsInfosDTO userSettingsInfosDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException;
    void deleteUser(UUID userID) throws ResourceNotFoundException;
    void addProductToWishlist(UUID userId, UUID productId) throws ResourceNotFoundException;
    void removeProductFromWishlist(UUID userId, UUID productId) throws ResourceNotFoundException;
    void clearWishlist(UUID userId) throws ResourceNotFoundException;
    Page<ProductListingDTO> getWishlist(UUID userId, Pageable pageable) throws ResourceNotFoundException;
    User getUserByEmail(String email) throws ResourceNotFoundException;
    List<UUID> getWishlistProductIds(UUID userId) throws ResourceNotFoundException;
    /**
     * Récupère l'utilisateur actuellement authentifié.
     *
     * @param authentication Objet d'authentification contenant les détails de l'utilisateur.
     * @return L'utilisateur authentifié.
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé.
     */
    User getUserMe(Authentication authentication) throws ResourceNotFoundException;

    /**
     * Supprime l'utilisateur actuellement authentifié.
     *
     * @param authentication Objet d'authentification contenant les détails de l'utilisateur.
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé.
     */
    void deleteUserMe(Authentication authentication) throws ResourceNotFoundException;

    /**
     * Change le mot de passe de l'utilisateur actuellement authentifié.
     *
     * @param changePasswordRequestDTO Les détails du changement de mot de passe.
     * @param authentication          L'objet d'authentification contenant les détails de l'utilisateur.
     * @throws ResourceNotFoundException Si l'utilisateur authentifié n'est pas trouvé.
     * @throws BadCredentialsException   Si le mot de passe actuel est incorrect.
     * @throws ResourceNotValidException Si le nouveau mot de passe ne respecte pas les contraintes.
     */
    void changePassword(ChangePasswordRequestDTO changePasswordRequestDTO, Authentication authentication)
            throws ResourceNotFoundException, BadCredentialsException, ResourceNotValidException;
}