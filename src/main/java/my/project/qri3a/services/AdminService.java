package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.UserUpdateRequestDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AdminService {

    /**
     * Récupère une liste paginée de tous les utilisateurs
     *
     * @param pageable paramètres de pagination
     * @return Page d'utilisateurs
     * @throws ResourceNotValidException si les paramètres de tri sont invalides
     */
    Page<User> getPaginatedUsers(Pageable pageable) throws ResourceNotValidException;

    /**
     * Crée un nouvel utilisateur (admin only)
     *
     * @param user l'utilisateur à créer
     * @return l'utilisateur créé
     * @throws ResourceAlreadyExistsException si un utilisateur avec le même email existe déjà
     * @throws ResourceNotValidException si les données de l'utilisateur sont invalides
     */
    User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException;

    /**
     * Met à jour un utilisateur existant (admin only)
     *
     * @param userID l'ID de l'utilisateur à mettre à jour
     * @param userRequestDTO les données mises à jour de l'utilisateur
     * @return l'utilisateur mis à jour
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     * @throws ResourceNotValidException si les données de l'utilisateur sont invalides
     */
    User updateUser(UUID userID, UserUpdateRequestDTO userRequestDTO)
            throws ResourceNotFoundException, ResourceNotValidException, AccessDeniedException;

    /**
     * Supprime un utilisateur (admin only)
     * Un admin ne peut pas supprimer son propre compte
     *
     * @param userID l'ID de l'utilisateur à supprimer
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     * @throws AccessDeniedException si l'admin essaie de supprimer son propre compte
     */
    void deleteUser(UUID userID) throws ResourceNotFoundException, AccessDeniedException,AccessDeniedException;

    /**
     * Bloque ou débloque un utilisateur (admin only)
     * Un admin ne peut pas bloquer son propre compte
     *
     * @param userID l'ID de l'utilisateur à bloquer/débloquer
     * @param blocked true pour bloquer, false pour débloquer
     * @return l'utilisateur mis à jour
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     * @throws AccessDeniedException si l'admin essaie de bloquer son propre compte
     */
    User blockUser(UUID userID, boolean blocked) throws ResourceNotFoundException, AccessDeniedException;


    Page<User> searchUsers(
            String name,
            String email,
            String phone,
            String role,
            Boolean status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable) throws ResourceNotValidException;

    User resetPassword(UUID userID, String newPassword) throws ResourceNotFoundException;
}