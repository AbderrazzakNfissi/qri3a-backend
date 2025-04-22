package my.project.qri3a.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import my.project.qri3a.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;

public interface ProductService {
    Page<ProductListingDTO> getAllProducts(Pageable pageable, String category, String location, String condition, UUID sellerId, BigDecimal minPrice, BigDecimal maxPrice, String city) throws ResourceNotValidException;
    ProductResponseDTO getProductById(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;
    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException;
    ProductResponseDTO updateMyProduct(UUID productId, ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;
    void deleteProduct(UUID productId) throws ResourceNotFoundException;
    Page<ProductListingDTO> getMyProducts(Authentication authentication, Pageable pageable) throws ResourceNotValidException;
    void deleteMyProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;
    Page<ProductListingDTO> getRecommendedProducts(UUID productId, Pageable pageable) throws ResourceNotFoundException;

    Page<ProductListingDTO> searchProducts(String query, Pageable pageable);
    Page<ProductListingDTO> searchProducts(
            String query,
            Pageable pageable,
            String category,
            String location,
            String condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String city
    );
    Page<ProductListingDTO> getProductsByUserId(UUID userId, Pageable pageable) throws ResourceNotFoundException;

    Page<ProductDoc> searchProductsElastic(String query, Pageable pageable, String category, String location, String condition, BigDecimal minPrice, BigDecimal maxPrice, String city,  String delivery) ;

    Page<ProductDoc> findAll(Pageable pageable);

    public List<ProductDoc> searchProductSuggestionsElastic(String query, String category, int limit);

    /**
     * Approves a product, changing its status to ACTIVE
     * @param productId The ID of the product to approve
     * @return The updated product details
     * @throws ResourceNotFoundException if the product is not found
     * @throws NotAuthorizedException if the user is not authorized to approve products
     */
    ProductResponseDTO approveProduct(UUID productId) throws ResourceNotFoundException, NotAuthorizedException;

    /**
     * Rejects a product, changing its status to REJECTED
     * @param productId The ID of the product to reject
     * @return The updated product details
     * @throws ResourceNotFoundException if the product is not found
     * @throws NotAuthorizedException if the user is not authorized to reject products
     */
    ProductResponseDTO rejectProduct(UUID productId) throws ResourceNotFoundException, NotAuthorizedException;

    /**
     * Get products filtered by status with pagination
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of products with the specified status
     */
    Page<ProductListingDTO> getProductsByStatus(ProductStatus status, Pageable pageable);

    /**
     * Get products that are in ACTIVE status
     * @param pageable Pagination information
     * @return Page of active products
     */
    Page<ProductListingDTO> getActiveProducts(Pageable pageable);

    /**
     * Get products that are in MODERATION status
     * @param pageable Pagination information
     * @return Page of products in moderation
     */
    Page<ProductListingDTO> getModerationProducts(Pageable pageable);

    /**
     * Get products that are in REJECTED status
     * @param pageable Pagination information
     * @return Page of rejected products
     */
    Page<ProductListingDTO> getRejectedProducts(Pageable pageable);

    /**
     * Get authenticated user's products filtered by ACTIVE status
     * @param authentication Authentication details of the user
     * @param pageable Pagination information
     * @return Page of active products belonging to the authenticated user
     * @throws ResourceNotFoundException if the user is not found
     */
    Page<ProductListingDTO> getMyActiveProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException;

    /**
     * Get authenticated user's products filtered by MODERATION status
     * @param authentication Authentication details of the user
     * @param pageable Pagination information
     * @return Page of products in moderation belonging to the authenticated user
     * @throws ResourceNotFoundException if the user is not found
     */
    Page<ProductListingDTO> getMyModerationProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException;

    /**
     * Get authenticated user's products filtered by REJECTED status
     * @param authentication Authentication details of the user
     * @param pageable Pagination information
     * @return Page of rejected products belonging to the authenticated user
     * @throws ResourceNotFoundException if the user is not found
     */
    Page<ProductListingDTO> getMyRejectedProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException;

    /**
     * Get counts of products by status for the authenticated user
     * @param authentication Authentication details of the user
     * @return Map of product status to count
     * @throws ResourceNotFoundException if the user is not found
     */
    Map<ProductStatus, Long> getMyProductCounts(Authentication authentication) throws ResourceNotFoundException;

    /**
     * Get authenticated user's products filtered by DEACTIVATED status
     * @param authentication Authentication details of the user
     * @param pageable Pagination information
     * @return Page of deactivated products belonging to the authenticated user
     * @throws ResourceNotFoundException if the user is not found
     */
    Page<ProductListingDTO> getMyDeactivatedProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException;

    /**
     * Deactivate a product (change status to DEACTIVATED)
     * @param productId The ID of the product to deactivate
     * @param authentication Authentication details of the user
     * @return The updated product details
     * @throws ResourceNotFoundException if the product is not found
     * @throws NotAuthorizedException if the user is not authorized to deactivate the product
     */
    ProductResponseDTO deactivateProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;

    ProductResponseDTO activateProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;


    /**
     * Récupère le nombre de produits pour un statut donné
     */
    long getProductCountByStatus(ProductStatus status);

    /**
     * Récupère le nombre de produits pour chaque statut
     */
    Map<ProductStatus, Long> getAllProductCounts();

    /**
     * Récupère les produits filtrés par catégorie principale
     * @param mainCategory Le nom de la catégorie principale
     * @param pageable Les informations de pagination
     * @return Page de produits correspondant à la catégorie principale
     */
    Page<ProductListingDTO> getProductsByMainCategory(String mainCategory, Pageable pageable);

    /**
     * Récupère un produit par son slug SEO-friendly
     * @param slug Le slug du produit à récupérer
     * @param authentication Les informations d'authentification de l'utilisateur
     * @return Les détails du produit
     * @throws ResourceNotFoundException si le produit n'est pas trouvé
     */
    ProductResponseDTO getProductBySlug(String slug, Authentication authentication) throws ResourceNotFoundException;

    /**
     * Approves a batch of products, changing their status to ACTIVE
     * @param productIds List of product IDs to approve
     * @return Number of products successfully approved
     */
    int approveBatchProducts(List<UUID> productIds);
}
