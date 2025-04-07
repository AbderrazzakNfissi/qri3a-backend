package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.UserPreferenceDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface UserPreferenceService {
    UserPreferenceDTO getPreference(UUID userId, String key) throws ResourceNotFoundException;
    List<UserPreferenceDTO> getAllPreferences(UUID userId) throws ResourceNotFoundException;
    UserPreferenceDTO updatePreference(UUID userId, UserPreferenceDTO preference) throws ResourceNotFoundException;
    void deletePreference(UUID userId, String key) throws ResourceNotFoundException;

    // Méthodes pour l'utilisateur authentifié
    UserPreferenceDTO getMyPreference(String key, Authentication authentication) throws ResourceNotFoundException;
    List<UserPreferenceDTO> getAllMyPreferences(Authentication authentication) throws ResourceNotFoundException;
    UserPreferenceDTO updateMyPreference(UserPreferenceDTO preference, Authentication authentication) throws ResourceNotFoundException;
    void deleteMyPreference(String key, Authentication authentication) throws ResourceNotFoundException;
}