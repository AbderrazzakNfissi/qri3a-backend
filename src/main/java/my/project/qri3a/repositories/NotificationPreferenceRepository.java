package my.project.qri3a.repositories;

import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUser(User user);

    Optional<NotificationPreference> findByIdAndUser(UUID id, User user);

    boolean existsByIdAndUser(UUID id, User user);
}