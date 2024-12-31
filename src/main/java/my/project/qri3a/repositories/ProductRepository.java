package my.project.qri3a.repositories;

import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findBySeller(User seller, Pageable pageable);

    @Query("SELECT p FROM User u JOIN u.wishlist p WHERE u.id = :userId")
    Page<Product> findWishlistByUserId(@Param("userId") UUID userId, Pageable pageable);


}
