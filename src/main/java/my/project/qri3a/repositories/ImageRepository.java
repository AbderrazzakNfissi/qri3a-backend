package my.project.qri3a.repositories;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import my.project.qri3a.entities.Image;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
    /**
     * Find an image by Product ID and Image ID.
     *
     * @param productId ID of the product.
     * @param imageId   ID of the image.
     * @return Optional containing the image if found.
     */
    Optional<Image> findByProductIdAndId(UUID productId, UUID imageId);

    /**
     * Count the number of images associated with a product.
     *
     * @param productId ID of the product.
     * @return Number of images.
     */
    long countByProductId(UUID productId);


    @Query("SELECT i.url FROM Image i JOIN i.product p WHERE p.seller.id = :userId")
    List<String> findImageUrlsBySellerId(@Param("userId") UUID userId);

    /**
     * Find all images associated with a product.
     *
     * @param productId ID of the product.
     * @return List of images.
     */
    List<Image> findByProductId(UUID productId);

    @Query("SELECT i FROM Image i JOIN i.product p WHERE p.seller.id = :sellerId AND p.status = 'ACTIVE' ORDER BY i.createdAt DESC")
    List<Image> findTop3ByProductSellerIdOrderByCreatedAtDesc(@Param("sellerId") UUID sellerId);
}
