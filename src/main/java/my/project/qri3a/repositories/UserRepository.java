package my.project.qri3a.repositories;

import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    
    List<User> findByRole(Role role);

    @Modifying
    @Query(value = "DELETE FROM user_wishlist WHERE product_id IN (SELECT id FROM products WHERE seller_id = :userId)", nativeQuery = true)
    void deleteWishlistEntriesForSellerProducts(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM user_wishlist WHERE user_id = :userId", nativeQuery = true)
    void deleteAllUserWishlistEntries(@Param("userId") UUID userId);

    /**
     * Trouve tous les vendeurs actifs qui ont au moins un produit actif
     * Utilisé pour la génération du sitemap
     * 
     * @return Liste des vendeurs actifs avec des produits
     */
    @Query("SELECT DISTINCT u FROM User u JOIN Product p ON p.seller.id = u.id WHERE p.status = 'ACTIVE' AND u.blocked = false")
    List<User> findActiveSellersWithProducts();
}