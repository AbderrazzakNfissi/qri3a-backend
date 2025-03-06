package my.project.qri3a.repositories;

import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUser(User user);

    Optional<NotificationPreference> findByIdAndUser(UUID id, User user);

    boolean existsByIdAndUser(UUID id, User user);

    // Nouvelle méthode pour trouver les utilisateurs intéressés par un produit spécifique
    @Query("SELECT DISTINCT np.user FROM NotificationPreference np WHERE " +
            "(np.productCategory IS NULL OR np.productCategory = :category) AND " +
            "(np.productState IS NULL OR np.productState = :condition) AND " +
            "(np.city IS NULL OR np.city = '' OR LOWER(np.city) = LOWER(:city)) AND " +
            "(np.minPrice IS NULL OR np.minPrice <= :price) AND " +
            "(np.maxPrice IS NULL OR np.maxPrice >= :price)")
    List<User> findInterestedUsersByProductCriteria(
            @Param("category") ProductCategory category,
            @Param("condition") ProductCondition condition,
            @Param("city") String city,
            @Param("price") BigDecimal price);
}