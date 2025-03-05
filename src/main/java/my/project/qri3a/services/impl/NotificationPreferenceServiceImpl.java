package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.NotificationPreferenceRepository;
import my.project.qri3a.services.NotificationPreferenceService;
import my.project.qri3a.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserService userService;

    @Override
    public NotificationPreference createNotificationPreference(NotificationPreference notificationPreference, Authentication authentication)
            throws ResourceNotFoundException {
        log.info("Creating notification preference for authenticated user");

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        notificationPreference.setUser(user);
        NotificationPreference savedPreference = notificationPreferenceRepository.save(notificationPreference);

        log.info("Notification preference created with ID: {}", savedPreference.getId());
        return savedPreference;
    }

    @Override
    public NotificationPreference updateNotificationPreference(UUID id, NotificationPreference notificationPreference, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Updating notification preference with ID: {}", id);

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        NotificationPreference existingPreference = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found with ID: " + id));

        // Vérifier que l'utilisateur est autorisé à modifier cette préférence
        if (!existingPreference.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to update notification preference with ID: {}", id);
            throw new NotAuthorizedException("You are not authorized to update this notification preference");
        }

        // Mettre à jour les champs
        existingPreference.setProductCategory(notificationPreference.getProductCategory());
        existingPreference.setMinPrice(notificationPreference.getMinPrice());
        existingPreference.setMaxPrice(notificationPreference.getMaxPrice());
        existingPreference.setCity(notificationPreference.getCity());

        NotificationPreference updatedPreference = notificationPreferenceRepository.save(existingPreference);
        log.info("Notification preference updated with ID: {}", updatedPreference.getId());

        return updatedPreference;
    }

    @Override
    public void deleteNotificationPreference(UUID id, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Deleting notification preference with ID: {}", id);

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        NotificationPreference preference = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found with ID: " + id));

        // Vérifier que l'utilisateur est autorisé à supprimer cette préférence
        if (!preference.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to delete notification preference with ID: {}", id);
            throw new NotAuthorizedException("You are not authorized to delete this notification preference");
        }

        notificationPreferenceRepository.delete(preference);
        log.info("Notification preference deleted with ID: {}", id);
    }

    @Override
    public List<NotificationPreference> getCurrentUserNotificationPreferences(Authentication authentication)
            throws ResourceNotFoundException {
        log.info("Fetching notification preferences for authenticated user");

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        List<NotificationPreference> preferences = notificationPreferenceRepository.findByUser(user);
        log.info("Found {} notification preferences for user", preferences.size());

        return preferences;
    }

    @Override
    public NotificationPreference getNotificationPreferenceById(UUID id, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Fetching notification preference with ID: {} for authenticated user", id);

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        NotificationPreference preference = notificationPreferenceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found with ID: " + id));

        return preference;
    }
}