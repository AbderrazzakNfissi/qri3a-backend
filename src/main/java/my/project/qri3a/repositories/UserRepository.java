package my.project.qri3a.repositories;

import my.project.qri3a.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = "DELETE FROM user_wishlist WHERE product_id IN (SELECT id FROM products WHERE seller_id = :userId)", nativeQuery = true)
    void deleteWishlistEntriesForSellerProducts(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM user_wishlist WHERE user_id = :userId", nativeQuery = true)
    void deleteAllUserWishlistEntries(@Param("userId") UUID userId);


}