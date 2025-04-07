package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.UserPreferenceDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.services.UserPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/me")
    public ResponseEntity<List<UserPreferenceDTO>> getMyPreferences(Authentication authentication) throws ResourceNotFoundException {
        List<UserPreferenceDTO> preferences = userPreferenceService.getAllMyPreferences(authentication);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/me/{key}")
    public ResponseEntity<UserPreferenceDTO> getMyPreference(@PathVariable String key, Authentication authentication) throws ResourceNotFoundException {
        UserPreferenceDTO preference = userPreferenceService.getMyPreference(key, authentication);
        return ResponseEntity.ok(preference);
    }

    @PutMapping("/me")
    public ResponseEntity<UserPreferenceDTO> updateMyPreference(@RequestBody UserPreferenceDTO preference, Authentication authentication) throws ResourceNotFoundException {
        UserPreferenceDTO updatedPreference = userPreferenceService.updateMyPreference(preference, authentication);
        return ResponseEntity.ok(updatedPreference);
    }

    @DeleteMapping("/me/{key}")
    public ResponseEntity<Void> deleteMyPreference(@PathVariable String key, Authentication authentication) throws ResourceNotFoundException {
        userPreferenceService.deleteMyPreference(key, authentication);
        return ResponseEntity.noContent().build();
    }
}