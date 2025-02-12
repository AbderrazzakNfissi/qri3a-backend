package my.project.qri3a.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.NotificationResponseDTO;
import my.project.qri3a.entities.Notification;
import my.project.qri3a.entities.User;
import my.project.qri3a.mappers.NotificationMapper;
import my.project.qri3a.repositories.NotificationRepository;
import my.project.qri3a.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;  // Inject the mapper

    @Override
    public Notification createNotification(Notification notification) {
        // Persist the notification (ID generation and createdAt handled automatically, if applicable)
        Notification savedNotification = notificationRepository.save(notification);

        // Use the mapper to convert to a DTO including the first product image if available
        NotificationResponseDTO dto = notificationMapper.toDTO(savedNotification);

        // Publish the notification via WebSocket (it will be converted to JSON)
        messagingTemplate.convertAndSend("/topic/notifications", dto);
        return savedNotification;
    }

    @Override
    public Optional<Notification> getNotificationById(UUID id) {
        return notificationRepository.findById(id);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification updateNotification(UUID id, Notification notification) {
        return notificationRepository.findById(id)
                .map(existing -> {
                    existing.setBody(notification.getBody());
                    existing.setCategory(notification.getCategory());
                    existing.setProduct(notification.getProduct());
                    existing.setUser(notification.getUser());
                    existing.setRead(notification.isRead());
                    // Do not update createdAt to preserve the original creation date
                    return notificationRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
    }

    @Override
    public void deleteNotification(UUID id) {
        notificationRepository.deleteById(id);
    }


    @Override
    public Page<Notification> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUser(user, pageable);
    }
}
