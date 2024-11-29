package my.project.qri3a.services;

import my.project.qri3a.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    List<User> getAllUsers();
    Optional<User> getUserById(UUID userID);
    User createUser(User user);
    User updateUser(UUID userID, User userDetails);
    void deleteUser(UUID userID);
}
