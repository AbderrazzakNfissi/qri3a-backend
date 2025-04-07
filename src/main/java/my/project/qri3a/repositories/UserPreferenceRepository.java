package my.project.qri3a.repositories;

import my.project.qri3a.entities.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    List<UserPreference> findByUserId(UUID userId);
    Optional<UserPreference> findByUserIdAndKey(UUID userId, String key);
}