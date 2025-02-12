package my.project.qri3a.repositories;

import my.project.qri3a.entities.Notification;
import my.project.qri3a.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUser(User user, Pageable pageable);

}
