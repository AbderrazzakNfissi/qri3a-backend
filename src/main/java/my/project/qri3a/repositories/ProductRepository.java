package my.project.qri3a.repositories;

import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findBySeller(User seller, Pageable pageable);
    Page<Product> findBySellerOrderByCreatedAtDesc(User seller, Pageable pageable);

    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM User u JOIN u.wishlist p WHERE u.id = :userId")
    Page<Product> findWishlistByUserId(@Param("userId") UUID userId, Pageable pageable);

     @Query(
                value = "SELECT p FROM Product p " +
                        "WHERE p.id <> :productId " +
                        "ORDER BY " +
                        "CASE WHEN p.category = :category THEN 0 ELSE 1 END, " +
                        "ABS(p.price - :price)",
                countQuery = "SELECT COUNT(p) FROM Product p WHERE p.id <> :productId"
        )
        Page<Product> findRecommendedProducts(
                @Param("category") ProductCategory category,
                @Param("price") BigDecimal price,
                @Param("productId") UUID productId,
                Pageable pageable
        );

    List<Product> findTop10ByTitleContainingIgnoreCase(String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Page<Product> searchProducts(
            @Param("query") String query,
            Pageable pageable
    );

    long countBySellerId(UUID sellerId);

    /**
     * Find products by status, ordered by creation date (newest first)
     * @param status The product status to filter by
     * @param pageable Pagination information
     * @return Page of products with the specified status
     */
    Page<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    /**
     * Find products by seller and status, ordered by creation date (newest first)
     * @param seller The seller/owner of the products
     * @param status The product status to filter by
     * @param pageable Pagination information
     * @return Page of products with the specified seller and status
     */
    Page<Product> findBySellerAndStatusOrderByCreatedAtDesc(User seller, ProductStatus status, Pageable pageable);

    /**
     * Count products by seller and group by status
     * @param sellerId The ID of the seller/owner
     * @return List of Object arrays containing status and count
     */
    @Query("SELECT p.status, COUNT(p) FROM Product p WHERE p.seller.id = :sellerId GROUP BY p.status")
    List<Object[]> countBySellerAndGroupByStatus(@Param("sellerId") UUID sellerId);

    @Modifying
    @Query(value = "DELETE FROM user_wishlist WHERE product_id = :productId", nativeQuery = true)
    void removeProductFromAllWishlists(@Param("productId") UUID productId);
}
