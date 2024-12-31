package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ProductMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ProductService;
import my.project.qri3a.specifications.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    public Page<ProductListingDTO> getAllProducts(Pageable pageable, String category, String location, String condition, UUID sellerId, BigDecimal minPrice, BigDecimal maxPrice, String city) throws ResourceNotValidException {
        log.info("Service: Fetching all products with filters - category: {}, location: {}, condition: {}, sellerId: {}, minPrice: {}, maxPrice: {}, city{}",
                category, location, condition, sellerId, minPrice, maxPrice,city);

        Specification<Product> spec = Specification.where(null);

        if (category != null && !category.isEmpty()) {
            try {
                spec = spec.and(ProductSpecifications.hasCategory(ProductCategory.valueOf(category.toUpperCase())));
            } catch (IllegalArgumentException ex) {
                log.error("Invalid category: {}", category);
                throw new ResourceNotValidException("Invalid category: " + category);
            }
        }

        if (location != null && !location.isEmpty()) {
            spec = spec.and(ProductSpecifications.hasLocation(location));
        }

        if (condition != null && !condition.isEmpty()) {
            try {
                spec = spec.and(ProductSpecifications.hasCondition(ProductCondition.valueOf(condition.toUpperCase())));
            } catch (IllegalArgumentException ex) {
                log.error("Invalid condition: {}", condition);
                throw new ResourceNotValidException("Invalid condition: " + condition);
            }
        }

        if (sellerId != null) {
            spec = spec.and(ProductSpecifications.hasSellerId(sellerId));
        }

        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.hasMinPrice(minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.hasMaxPrice(maxPrice));
        }

        if (city != null) {
            spec = spec.and(ProductSpecifications.hasCity(city));
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.error("minPrice {} is greater than maxPrice {}", minPrice, maxPrice);
            throw new ResourceNotValidException("minPrice cannot be greater than maxPrice");
        }
        Page<Product> productsPage = productRepository.findAll(spec, pageable);
        log.info("Service: Found {} products", productsPage.getTotalElements());

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public ProductResponseDTO getProductById(UUID productId) throws ResourceNotFoundException {
        log.info("Service: Fetching product with ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });
        return productMapper.toDTO(product);
    }

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Creating product with title: {}", productRequestDTO.getTitle());

        // Vérifier que le vendeur existe
        UUID sellerId = productRequestDTO.getSellerId();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> {
                    log.warn("Service: Seller not found with ID: {}", sellerId);
                    return new ResourceNotFoundException("Seller not found with ID " + sellerId);
                });

        // Mapper le DTO vers l'entité
        Product product = productMapper.toEntity(productRequestDTO);
        product.setSeller(seller);

        // Enregistrer le produit
        Product savedProduct = productRepository.save(product);
        log.info("Service: Product created with ID: {}", savedProduct.getId());

        return productMapper.toDTO(savedProduct);
    }

    @Override
    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO productRequestDTO) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Updating product with ID: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Si le sellerId est mis à jour, vérifier que le nouveau vendeur existe
        if (!existingProduct.getSeller().getId().equals(productRequestDTO.getSellerId())) {
            User newSeller = userRepository.findById(productRequestDTO.getSellerId())
                    .orElseThrow(() -> {
                        log.warn("Service: New seller not found with ID: {}", productRequestDTO.getSellerId());
                        return new ResourceNotFoundException("Seller not found with ID " + productRequestDTO.getSellerId());
                    });
            existingProduct.setSeller(newSeller);
        }

        // Mettre à jour les champs de l'entité à partir du DTO
        productMapper.updateEntityFromDTO(productRequestDTO, existingProduct);

        // Enregistrer le produit mis à jour
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Service: Product updated with ID: {}", updatedProduct.getId());

        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public void deleteProduct(UUID productId) throws ResourceNotFoundException {
        log.info("Service: Deleting product with ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });
        productRepository.delete(product);
        log.info("Service: Product deleted with ID: {}", productId);
    }
}
