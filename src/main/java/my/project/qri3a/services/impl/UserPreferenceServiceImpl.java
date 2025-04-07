package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.UserPreferenceDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.entities.UserPreference;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.UserPreferenceRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.UserPreferenceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public UserPreferenceDTO getPreference(UUID userId, String key) throws ResourceNotFoundException {
        log.info("Service: Fetching preference with key {} for user with ID {}", key, userId);

        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        // Chercher la préférence
        UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
                .orElseThrow(() -> {
                    log.warn("Service: Preference with key {} not found for user with ID {}", key, userId);
                    return new ResourceNotFoundException("Preference with key " + key + " not found");
                });

        return toDTO(preference);
    }

    @Override
    public List<UserPreferenceDTO> getAllPreferences(UUID userId) throws ResourceNotFoundException {
        log.info("Service: Fetching all preferences for user with ID {}", userId);

        // Vérifier que l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            log.warn("Service: User not found with ID: {}", userId);
            throw new ResourceNotFoundException("User not found with ID " + userId);
        }

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        return preferences.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public UserPreferenceDTO updatePreference(UUID userId, UserPreferenceDTO preferenceDTO) throws ResourceNotFoundException {
        log.info("Service: Updating preference with key {} for user with ID {}", preferenceDTO.getKey(), userId);

        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        // Chercher la préférence existante ou en créer une nouvelle
        Optional<UserPreference> existingPreference = userPreferenceRepository.findByUserIdAndKey(userId, preferenceDTO.getKey());

        UserPreference preference;
        if (existingPreference.isPresent()) {
            preference = existingPreference.get();
            preference.setValue(preferenceDTO.getValue());
        } else {
            preference = new UserPreference();
            preference.setUser(user);
            preference.setKey(preferenceDTO.getKey());
            preference.setValue(preferenceDTO.getValue());
        }

        preference = userPreferenceRepository.save(preference);
        log.info("Service: Preference updated for user with ID {}", userId);

        return toDTO(preference);
    }

    @Override
    public void deletePreference(UUID userId, String key) throws ResourceNotFoundException {
        log.info("Service: Deleting preference with key {} for user with ID {}", key, userId);

        // Vérifier que l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            log.warn("Service: User not found with ID: {}", userId);
            throw new ResourceNotFoundException("User not found with ID " + userId);
        }

        // Chercher la préférence
        UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
                .orElseThrow(() -> {
                    log.warn("Service: Preference with key {} not found for user with ID {}", key, userId);
                    return new ResourceNotFoundException("Preference with key " + key + " not found");
                });

        userPreferenceRepository.delete(preference);
        log.info("Service: Preference with key {} deleted for user with ID {}", key, userId);
    }

    @Override
    public UserPreferenceDTO getMyPreference(String key, Authentication authentication) throws ResourceNotFoundException {
        User user = getUserFromAuthentication(authentication);
        return getPreference(user.getId(), key);
    }

    @Override
    public List<UserPreferenceDTO> getAllMyPreferences(Authentication authentication) throws ResourceNotFoundException {
        User user = getUserFromAuthentication(authentication);
        return getAllPreferences(user.getId());
    }

    @Override
    public UserPreferenceDTO updateMyPreference(UserPreferenceDTO preference, Authentication authentication) throws ResourceNotFoundException {
        User user = getUserFromAuthentication(authentication);
        return updatePreference(user.getId(), preference);
    }

    @Override
    public void deleteMyPreference(String key, Authentication authentication) throws ResourceNotFoundException {
        User user = getUserFromAuthentication(authentication);
        deletePreference(user.getId(), key);
    }

    private User getUserFromAuthentication(Authentication authentication) throws ResourceNotFoundException {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Service: Authenticated user not found with email: {}", email);
                    return new ResourceNotFoundException("Authenticated user not found");
                });
    }

    private UserPreferenceDTO toDTO(UserPreference preference) {
        UserPreferenceDTO dto = new UserPreferenceDTO();
        dto.setKey(preference.getKey());
        dto.setValue(preference.getValue());
        return dto;
    }
}