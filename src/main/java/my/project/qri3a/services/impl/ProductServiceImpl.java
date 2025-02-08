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
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ProductMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ProductService;
import my.project.qri3a.services.UserService;
import my.project.qri3a.specifications.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final UserService userService;

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
    public Page<ProductListingDTO> searchProducts(String query, Pageable pageable, String category, String location, String condition, BigDecimal minPrice, BigDecimal maxPrice, String city) {
        log.info("Service: Recherche des produits avec le terme: {} et filtres - category: {}, location: {}, condition: {}, minPrice: {}, maxPrice: {}, city: {}",
                query, category, location, condition, minPrice, maxPrice, city);

        // Démarrer avec une spécification sur le texte (titre ou description)
        Specification<Product> spec = Specification.where(ProductSpecifications.containsText(query));

        // Filtre sur la catégorie
        if (category != null && !category.isEmpty()) {
            try {
                spec = spec.and(ProductSpecifications.hasCategory(ProductCategory.valueOf(category.toUpperCase())));
            } catch (IllegalArgumentException ex) {
                log.error("Invalid category: {}", category);
                throw new ResourceNotValidException("Invalid category: " + category);
            }
        }

        // Filtre sur la location (si vous avez une spécification associée)
        if (location != null && !location.isEmpty()) {
            spec = spec.and(ProductSpecifications.hasLocation(location));
        }

        // Filtre sur la condition
        if (condition != null && !condition.isEmpty()) {
            try {
                spec = spec.and(ProductSpecifications.hasCondition(ProductCondition.valueOf(condition.toUpperCase())));
            } catch (IllegalArgumentException ex) {
                log.error("Invalid condition: {}", condition);
                throw new ResourceNotValidException("Invalid condition: " + condition);
            }
        }

        // Filtre sur le prix minimum
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.hasMinPrice(minPrice));
        }

        // Filtre sur le prix maximum
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.hasMaxPrice(maxPrice));
        }

        // Filtre sur la ville
        if (city != null && !city.isEmpty()) {
            spec = spec.and(ProductSpecifications.hasCity(city));
        }

        // Vérifier que minPrice n'est pas supérieur à maxPrice
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.error("minPrice {} is greater than maxPrice {}", minPrice, maxPrice);
            throw new ResourceNotValidException("minPrice cannot be greater than maxPrice");
        }

        Page<Product> productsPage = productRepository.findAll(spec, pageable);
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
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Creating product with title: {}", productRequestDTO.getTitle());

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);
        productRequestDTO.setSellerId(seller.getId());

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


    @Override
    public Page<ProductListingDTO> getMyProducts(Authentication authentication, Pageable pageable) throws ResourceNotValidException {
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);
        log.info("=> seller email: {}", seller.getEmail());
        Page<Product> productsPage = productRepository.findBySeller(seller, pageable);
        return productsPage.map(productMapper::toProductListingDTO);
    }


    @Override
    public void deleteMyProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Deleting my product with ID: {}", productId);

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        if (!product.getSeller().getId().equals(seller.getId())) {
            log.warn("Service: Unauthorized attempt to delete product with ID: {}", productId);
            throw new NotAuthorizedException("You are not authorized to delete this product");
        }

        productRepository.delete(product);
        log.info("Service: Product deleted with ID: {}", productId);
    }


    @Override
    public Page<ProductListingDTO> getRecommendedProducts(UUID productId, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Récupération des produits recommandés pour le produit ID: {}", productId);

        // Récupérer le produit actuel
        Product currentProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Produit non trouvé avec l'ID: {}", productId);
                    return new ResourceNotFoundException("Produit non trouvé avec l'ID " + productId);
                });

        ProductCategory category = currentProduct.getCategory();
        BigDecimal price = currentProduct.getPrice();

        // Récupérer les produits recommandés triés par même catégorie puis par proximité de prix
        Page<Product> recommendedPage = productRepository.findRecommendedProducts(category, price, productId, pageable);
        log.info("Service: {} produits recommandés trouvés", recommendedPage.getTotalElements());

        return recommendedPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public List<ProductListingDTO> searchProductSuggestions(String query, int limit) {
        log.info("Service: Recherche des suggestions de produits pour le terme: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        PageRequest pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findTop10ByTitleContainingIgnoreCase(query, pageable);

        return products.stream()
                .map(productMapper::toProductListingDTO)
                .collect(Collectors.toList());
    }


    @Override
    public Page<ProductListingDTO> searchProducts(String query, Pageable pageable) {
        log.info("Service: Recherche des produits pour le terme: {}", query);
        Page<Product> products = productRepository.searchProducts(query, pageable);
        return products.map(productMapper::toProductListingDTO);
    }

    @Override
    public Page<ProductListingDTO> getProductsByUserId(UUID userId, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching products for user ID: {} with pagination: {}", userId, pageable);

        // Fetch the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Service: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID " + userId);
                });

        // Fetch products by the seller (user)
        Page<Product> productsPage = productRepository.findBySeller(user, pageable);
        log.info("Service: Found {} products for user ID: {}", productsPage.getTotalElements(), userId);

        // Convert entities to DTOs
        return productsPage.map(productMapper::toProductListingDTO);
    }




}
