package my.project.qri3a.services;

import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceService {

    // Créer une nouvelle préférence de notification
    NotificationPreference createNotificationPreference(NotificationPreference notificationPreference, Authentication authentication)
            throws ResourceNotFoundException;

    // Mettre à jour une préférence de notification existante
    NotificationPreference updateNotificationPreference(UUID id, NotificationPreference notificationPreference, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException;

    // Supprimer une préférence de notification
    void deleteNotificationPreference(UUID id, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException;

    // Récupérer toutes les préférences de notification de l'utilisateur authentifié
    List<NotificationPreference> getCurrentUserNotificationPreferences(Authentication authentication)
            throws ResourceNotFoundException;

    // Récupérer une préférence de notification spécifique de l'utilisateur authentifié
    NotificationPreference getNotificationPreferenceById(UUID id, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException;
}