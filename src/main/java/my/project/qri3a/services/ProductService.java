package my.project.qri3a.services;

import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.SellerProfileDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    Page<ProductListingDTO> getAllProducts(Pageable pageable, String category, String location, String condition, UUID sellerId, BigDecimal minPrice, BigDecimal maxPrice, String city) throws ResourceNotValidException;
    ProductResponseDTO getProductById(UUID productId) throws ResourceNotFoundException;
    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException;
    ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO productRequestDTO) throws ResourceNotFoundException, ResourceNotValidException;
    void deleteProduct(UUID productId) throws ResourceNotFoundException;
    Page<ProductListingDTO> getMyProducts(Authentication authentication, Pageable pageable) throws ResourceNotValidException;
    void deleteMyProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException;
    Page<ProductListingDTO> getRecommendedProducts(UUID productId, Pageable pageable) throws ResourceNotFoundException;
    List<ProductListingDTO> searchProductSuggestions(String query, int limit);
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

    Page<ProductDoc> searchProductsElastic(String query, Pageable pageable);

    Page<ProductDoc> findAll(Pageable pageable);
}
