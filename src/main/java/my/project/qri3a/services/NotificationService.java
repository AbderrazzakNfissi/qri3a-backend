package my.project.qri3a.services;
import my.project.qri3a.entities.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(Notification notification);
    Optional<Notification> getNotificationById(UUID id);
    List<Notification> getAllNotifications();
    Notification updateNotification(UUID id, Notification notification);
    void deleteNotification(UUID id);
}
