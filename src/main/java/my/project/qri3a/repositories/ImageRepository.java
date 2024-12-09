package my.project.qri3a.repositories;

import my.project.qri3a.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Find all images associated with a product.
     *
     * @param productId ID of the product.
     * @return List of images.
     */
    List<Image> findByProductId(UUID productId);
}
