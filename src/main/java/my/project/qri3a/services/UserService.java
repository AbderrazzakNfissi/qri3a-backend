package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserService {
    Page<User> getAllUsers(Pageable pageable) throws ResourceNotValidException ;
    Optional<User> getUserById(UUID userID) throws ResourceNotFoundException;
    User createUser(User user) throws ResourceAlreadyExistsException, ResourceNotValidException;
    User updateUser(UUID userID, UserRequestDTO userRequestDTO) throws ResourceNotFoundException, ResourceNotValidException;
    void deleteUser(UUID userID) throws ResourceNotFoundException;
    public void addProductToUser(UUID userId, UUID productId) throws ResourceNotFoundException;
    public void removeProductFromUser(UUID userId, UUID productId) throws ResourceNotFoundException;
    public void addProductToWishlist(UUID userId, UUID productId) throws ResourceNotFoundException;
    public void removeProductFromWishlist(UUID userId, UUID productId) throws ResourceNotFoundException;
}