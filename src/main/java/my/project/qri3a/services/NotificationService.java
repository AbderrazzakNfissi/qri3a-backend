package my.project.qri3a.services;
import my.project.qri3a.entities.Notification;
import my.project.qri3a.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(Notification notification);
    Optional<Notification> getNotificationById(UUID id);
    List<Notification> getAllNotifications();
    Notification updateNotification(UUID id, Notification notification);
    void deleteNotification(UUID id);
    Page<Notification> getMyNotifications(User user, Pageable pageable);
}
