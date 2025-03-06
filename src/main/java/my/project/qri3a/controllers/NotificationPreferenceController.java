package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.NotificationPreferenceDTO;
import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.NotificationPreferenceMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.NotificationPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationPreferenceMapper notificationPreferenceMapper;

    /**
     * GET /api/v1/notification-preferences
     * Récupère toutes les préférences de notification de l'utilisateur authentifié
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDTO>>> getCurrentUserNotificationPreferences(
            Authentication authentication) throws ResourceNotFoundException {

        log.info("Controller: Fetching notification preferences for authenticated user");

        List<NotificationPreference> preferences = notificationPreferenceService.getCurrentUserNotificationPreferences(authentication);
        List<NotificationPreferenceDTO> preferenceDTOs = preferences.stream()
                .map(notificationPreferenceMapper::toDTO)
                .collect(Collectors.toList());

        ApiResponse<List<NotificationPreferenceDTO>> response = new ApiResponse<>(
                preferenceDTOs,
                "Notification preferences fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/notification-preferences/{id}
     * Récupère une préférence de notification spécifique de l'utilisateur authentifié
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> getNotificationPreferenceById(
            @PathVariable UUID id,
            Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {

        log.info("Controller: Fetching notification preference with ID: {}", id);

        NotificationPreference preference = notificationPreferenceService.getNotificationPreferenceById(id, authentication);
        NotificationPreferenceDTO preferenceDTO = notificationPreferenceMapper.toDTO(preference);

        ApiResponse<NotificationPreferenceDTO> response = new ApiResponse<>(
                preferenceDTO,
                "Notification preference fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/notification-preferences
     * Crée une nouvelle préférence de notification pour l'utilisateur authentifié
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> createNotificationPreference(
            @Valid @RequestBody NotificationPreferenceDTO notificationPreferenceDTO,
            Authentication authentication) throws ResourceNotFoundException {

        log.info("Controller: Creating new notification preference");

        NotificationPreference notificationPreference = notificationPreferenceMapper.toEntity(notificationPreferenceDTO);
        NotificationPreference createdPreference = notificationPreferenceService.createNotificationPreference(notificationPreference, authentication);
        NotificationPreferenceDTO createdPreferenceDTO = notificationPreferenceMapper.toDTO(createdPreference);

        ApiResponse<NotificationPreferenceDTO> response = new ApiResponse<>(
                createdPreferenceDTO,
                "Notification preference created successfully.",
                HttpStatus.CREATED.value()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/notification-preferences/{id}
     * Met à jour une préférence de notification existante
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> updateNotificationPreference(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationPreferenceDTO notificationPreferenceDTO,
            Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {

        log.info("Controller: Updating notification preference with ID: {}", id);

        NotificationPreference notificationPreference = notificationPreferenceMapper.toEntity(notificationPreferenceDTO);
        NotificationPreference updatedPreference = notificationPreferenceService.updateNotificationPreference(id, notificationPreference, authentication);
        NotificationPreferenceDTO updatedPreferenceDTO = notificationPreferenceMapper.toDTO(updatedPreference);

        ApiResponse<NotificationPreferenceDTO> response = new ApiResponse<>(
                updatedPreferenceDTO,
                "Notification preference updated successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/notification-preferences/{id}
     * Supprime une préférence de notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotificationPreference(
            @PathVariable UUID id,
            Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {

        log.info("Controller: Deleting notification preference with ID: {}", id);

        notificationPreferenceService.deleteNotificationPreference(id, authentication);

        ApiResponse<Void> response = new ApiResponse<>(
                null,
                "Notification preference deleted successfully.",
                HttpStatus.NO_CONTENT.value()
        );

        return ResponseEntity.noContent().build();
    }
}