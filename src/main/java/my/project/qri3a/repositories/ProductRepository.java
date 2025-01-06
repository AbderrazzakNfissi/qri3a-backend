package my.project.qri3a.repositories;

import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findBySeller(User seller, Pageable pageable);

    @Query("SELECT p FROM User u JOIN u.wishlist p WHERE u.id = :userId")
    Page<Product> findWishlistByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Récupère les produits triés par même catégorie et proximité de prix.
     * Les produits de la même catégorie sont prioritaires et triés par proximité de prix,
     * suivis par les autres produits également triés par proximité de prix.
     *
     * @param category  La catégorie du produit actuel.
     * @param price     Le prix du produit actuel.
     * @param productId L'ID du produit actuel pour l'exclure des résultats.
     * @param pageable  Les paramètres de pagination.
     * @return Une page de produits recommandés.
     */
    @Query("SELECT p FROM Product p " +
            "WHERE p.id <> :productId " +
            "ORDER BY " +
            "CASE WHEN p.category = :category THEN 0 ELSE 1 END, " +
            "ABS(p.price - :price)")
    Page<Product> findRecommendedProducts(
            @Param("category") ProductCategory category,
            @Param("price") BigDecimal price,
            @Param("productId") UUID productId,
            Pageable pageable
    );
}
