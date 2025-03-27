package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.requests.UserSettingsInfosDTO;
import my.project.qri3a.dtos.requests.UserUpdateRequestDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {

    /**
     * Récupère une liste paginée de tous les utilisateurs
     *
     * @param pageable pagination parameters
     * @return Page of users
     * @throws ResourceNotValidException if sort parameters are invalid
     */
    Page<User> getPaginatedUsers(Pageable pageable) throws ResourceNotValidException;

    /**
     * Crée un nouvel utilisateur (admin only)
     *
     * @param user the user to create
     * @return the created user
     * @throws ResourceAlreadyExistsException if a user with the same email already exists
     * @throws ResourceNotValidException if user data is invalid
     */
    User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException;

    /**
     * Met à jour un utilisateur existant (admin only)
     *
     * @param userID the ID of the user to update
     * @param userRequestDTO the updated user data
     * @return the updated user
     * @throws ResourceNotFoundException if the user is not found
     * @throws ResourceNotValidException if user data is invalid
     */
    User updateUser(UUID userID, UserUpdateRequestDTO userRequestDTO)
            throws ResourceNotFoundException, ResourceNotValidException;

    /**
     * Supprime un utilisateur (admin only)
     *
     * @param userID the ID of the user to delete
     * @throws ResourceNotFoundException if the user is not found
     */
    void deleteUser(UUID userID) throws ResourceNotFoundException;

    /**
     * Bloque ou débloque un utilisateur (admin only)
     *
     * @param userID the ID of the user to block/unblock
     * @param blocked true to block, false to unblock
     * @return the updated user
     * @throws ResourceNotFoundException if the user is not found
     */
    User blockUser(UUID userID, boolean blocked) throws ResourceNotFoundException;
}