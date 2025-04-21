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
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findBySeller(User seller, Pageable pageable);
    Page<Product> findBySellerAndStatusOrderByCreatedAtDesc(User seller, ProductStatus status, Pageable pageable);

    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Nouvelle méthode pour récupérer les produits par status triés par nombre de vues croissant
    Page<Product> findByStatusOrderByViewsCountAsc(ProductStatus status, Pageable pageable);
    
    // Méthode pour les catégories principales avec tri par viewsCount
    Page<Product> findByStatusAndCategoryOrderByViewsCountAsc(ProductStatus status, ProductCategory category, Pageable pageable);
    
    // Méthode pour mettre à jour le viewsCount des produits en batch
    @Modifying
    @Query("UPDATE Product p SET p.viewsCount = p.viewsCount + :increment WHERE p.id IN :productIds")
    void incrementViewsCount(@Param("productIds") List<UUID> productIds, @Param("increment") int increment);

    @Query("SELECT p FROM User u JOIN u.wishlist p WHERE u.id = :userId")
    Page<Product> findWishlistByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = "SELECT p FROM Product p " +
                    "WHERE p.id <> :productId " +
                    "AND p.status = 'ACTIVE' " +
                    "ORDER BY " +
                    "CASE WHEN p.category = :category THEN 0 ELSE 1 END, " +
                    "ABS(p.price - :price)",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.id <> :productId AND p.status = 'ACTIVE'"
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

    long countBySellerIdAndStatus(UUID sellerId, ProductStatus status);

    /**
     * Find products by status, ordered by creation date (newest first)
     * @param status The product status to filter by
     * @param pageable Pagination information
     * @return Page of products with the specified status
     */
    Page<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    /**
     * Récupère tous les produits avec un statut spécifique
     * @param status Le statut des produits à récupérer
     * @return Liste des produits ayant le statut spécifié
     */
    List<Product> findByStatus(ProductStatus status);

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

    /**
     * Récupère tous les identifiants des produits pour un vendeur spécifique
     * @param sellerId L'identifiant du vendeur
     * @return Liste des UUIDs des produits appartenant au vendeur
     */
    @Query("SELECT p.id FROM Product p WHERE p.seller.id = :sellerId")
    List<UUID> findProductIdsBySellerId(@Param("sellerId") UUID sellerId);

    /**
     * Compte le nombre de produits pour un statut spécifique
     */
    long countByStatus(ProductStatus status);

    /**
     * Compte le nombre de produits groupés par statut
     */
    @Query("SELECT p.status, COUNT(p) FROM Product p GROUP BY p.status")
    List<Object[]> countGroupByStatus();

    /**
     * Trouve un produit par son slug SEO-friendly
     * @param slug Le slug à rechercher
     * @return Le produit correspondant au slug
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Trouve des produits par statut et catégorie, triés par date de création décroissante
     * @param status Le statut des produits
     * @param category La catégorie des produits
     * @param pageable Information de pagination
     * @return Une page de produits correspondant aux critères
     */
    Page<Product> findByStatusAndCategoryOrderByCreatedAtDesc(ProductStatus status, ProductCategory category, Pageable pageable);
}
