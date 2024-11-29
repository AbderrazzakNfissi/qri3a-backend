package my.project.qri3a.services.impl;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.UserService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    @Override
    public Optional<User> getUserById(UUID userID) {
        return userRepository.findById(userID);
    }


    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }


    @Override
    public User updateUser(UUID userID, User userDetails) {
        return userRepository.findById(userID)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }
                    user.setPhoneNumber(userDetails.getPhoneNumber());
                    user.setAddress(userDetails.getAddress());
                    user.setRole(userDetails.getRole());
                    user.setRating(userDetails.getRating());
                    // Les champs createdAt et updatedAt sont gérés automatiquement
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userID));
    }

    @Override
    public void deleteUser(UUID userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userID));
        userRepository.delete(user);
    }
}