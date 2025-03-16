package my.project.qri3a.services.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.services.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.entities.Image;
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
import my.project.qri3a.repositories.search.ProductDocRepository;
import my.project.qri3a.specifications.ProductSpecifications;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final UserService userService;
    private final ProductIndexService productIndexService;
    private final ProductDocRepository productDocRepository;
    private final S3Service s3Service;
    private final ProductMatchingService productMatchingService;

    @Override
    public Page<ProductListingDTO> getAllProducts(Pageable pageable, String category, String location, String condition, UUID sellerId, BigDecimal minPrice, BigDecimal maxPrice, String city) throws ResourceNotValidException {
        log.info("Service: Fetching all products with filters - category: {}, location: {}, condition: {}, sellerId: {}, minPrice: {}, maxPrice: {}, city{}",
                category, location, condition, sellerId, minPrice, maxPrice,city);

        Page<Product> productsPage = productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE,pageable);
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
    public ProductResponseDTO getProductById(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Si le produit n'est pas actif, vérifier si l'authentification existe
        if (product.getStatus() != ProductStatus.ACTIVE) {
            // Si l'utilisateur n'est pas authentifié, on rejette l'accès
            if (authentication == null) {
                log.warn("Service: Unauthenticated access attempt to non-active product with ID: {}", productId);
                throw new NotAuthorizedException("You must be logged in to view this product");
            }

            // Obtenir l'utilisateur authentifié
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            // Vérifier si l'utilisateur est le propriétaire du produit
            if (!product.getSeller().getId().equals(currentUser.getId())) {
                log.warn("Service: Unauthorized access to non-active product with ID: {}", productId);
                throw new NotAuthorizedException("You are not authorized to view this product");
            }
        }

        return productMapper.toDTO(product);
    }

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Creating product with title: {}", productRequestDTO.getTitle());

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Product product = productMapper.toEntity(productRequestDTO);
        product.setSeller(seller);

        // Enregistrer le produit
        Product savedProduct = productRepository.save(product);
        log.info("Service: Product created with ID: {}", savedProduct.getId());


        return productMapper.toDTO(savedProduct);
    }

    @Override
    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Service: Updating product with ID: {}", productId);

        // Récupérer l'utilisateur à partir de l'authentification
        String email = authentication.getName();
        User currentUser = userService.getUserByEmail(email);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Vérifier que l'utilisateur actuel est le propriétaire du produit
        if (!existingProduct.getSeller().getId().equals(currentUser.getId())) {
            log.warn("Service: User {} is not authorized to update product {}", currentUser.getId(), productId);
            throw new ResourceNotValidException("You are not authorized to update this product");
        }

        // Mettre à jour les champs de l'entité à partir du DTO
        productMapper.updateEntityFromDTO(productRequestDTO, existingProduct);

        // Enregistrer le produit mis à jour
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Service: Product updated with ID: {}", updatedProduct.getId());

        // Mettre à jour l'index Elasticsearch
        productIndexService.indexProduct(updatedProduct, 0);

        return productMapper.toDTO(updatedProduct);
    }



    //cette fonction il va etre utiliser par l'administrateur pour supprimer un produit
    @Override
    public void deleteProduct(UUID productId) throws ResourceNotFoundException {
        log.info("Service: Deleting product with ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });
        // Delete all associated images from S3
        for (Image image : product.getImages()) {
            String filename = image.getUrl().substring(image.getUrl().lastIndexOf('/') + 1);
            s3Service.deleteFile(filename);
        }
        productRepository.delete(product);
        log.info("Service: Product deleted with ID: {}", productId);
        productIndexService.deleteProductIndex(productId);
    }


    @Override
    public Page<ProductListingDTO> getMyProducts(Authentication authentication, Pageable pageable) throws ResourceNotValidException {
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);
        log.info("=> seller email: {}", seller.getEmail());
        Page<Product> productsPage = productRepository.findBySellerOrderByCreatedAtDesc(seller, pageable);
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
 
        for (Image image : product.getImages()) {
           
            String filename = image.getUrl().substring(image.getUrl().lastIndexOf('/') + 1);
            s3Service.deleteFile(filename);
        }

        productRepository.delete(product);
        log.info("Service: Product deleted with ID: {}", productId);

        productIndexService.deleteProductIndex(productId);
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


    /*
    @Override
    public Page<ProductDoc> searchProductsElastic(String query, Pageable pageable, String category, String location, String condition, BigDecimal minPrice, BigDecimal maxPrice, String city) {
        log.info("Service: Elasticsearch search with query: {} and filters - category: {}, location: {}, condition: {}, minPrice: {}, maxPrice: {}, city: {}",
                query, category, location, condition, minPrice, maxPrice, city);


        String safeCategory = category == null ? "" : category;
        String safeLocation = location == null ? "" : location;
        String safeCondition = condition == null ? "" : condition;
        String safeCity = city == null ? "" : city;
        BigDecimal safeMinPrice = minPrice == null ? BigDecimal.ZERO : minPrice;
        BigDecimal safeMaxPrice = maxPrice == null ? new BigDecimal("9999999") : maxPrice;

        return productDocRepository.searchProductsElastic(query, safeCategory, safeLocation, safeCondition,
                safeMinPrice, safeMaxPrice, safeCity, pageable);

    }
    */
    // Interface
    @Override
    public Page<ProductDoc> searchProductsElastic(String query, Pageable pageable, String category, String location, String condition, BigDecimal minPrice, BigDecimal maxPrice, String city, String delivery) {
        log.info("Service: Elasticsearch search with query: {} and filters - category: {}, location: {}, condition: {}, minPrice: {}, maxPrice: {}, city: {}, delivery: {}",
                query, category, location, condition, minPrice, maxPrice, city, delivery);

        return productDocRepository.searchProductsElastic(query, category, location, condition, minPrice, maxPrice, city, delivery, pageable);
    }


    @Override
    public Page<ProductDoc> findAll(Pageable pageable) {
        Page<ProductDoc> productDocs = productDocRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE.toString(),pageable);
        return productDocs;
    }

    @Override
    public List<ProductDoc> searchProductSuggestionsElastic(String query) {
        log.info("Service: Recherche des suggestions de produits pour le terme: {} - Using Elastic Search", query);

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductDoc> products = productDocRepository.findTop10ByTitleOrDescription(query);
        return products;
    }


    @Override
    public ProductResponseDTO approveProduct(UUID productId) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Approving product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Change product status to ACTIVE
        product.setStatus(ProductStatus.ACTIVE);
        Product updatedProduct = productRepository.save(product);
        log.info("Service: Product approved with ID: {}", updatedProduct.getId());

        // Update the Elasticsearch index
        productIndexService.indexProduct(updatedProduct, 0);

        // Notifier les utilisateurs intéressés
        productMatchingService.notifyInterestedUsers(updatedProduct);

        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public ProductResponseDTO rejectProduct(UUID productId) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Rejecting product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Change product status to REJECTED
        product.setStatus(ProductStatus.REJECTED);
        Product updatedProduct = productRepository.save(product);
        log.info("Service: Product rejected with ID: {}", updatedProduct.getId());

        // Update the Elasticsearch index
        productIndexService.indexProduct(updatedProduct, 0);

        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public Page<ProductListingDTO> getProductsByStatus(ProductStatus status, Pageable pageable) {
        log.info("Service: Fetching products with status: {}", status);

        Page<Product> productsPage = productRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        log.info("Service: Found {} products with status {}", productsPage.getTotalElements(), status);

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public Page<ProductListingDTO> getActiveProducts(Pageable pageable) {
        log.info("Service: Fetching active products");
        return getProductsByStatus(ProductStatus.ACTIVE, pageable);
    }

    @Override
    public Page<ProductListingDTO> getModerationProducts(Pageable pageable) {
        log.info("Service: Fetching products in moderation");
        return getProductsByStatus(ProductStatus.MODERATION, pageable);
    }

    @Override
    public Page<ProductListingDTO> getRejectedProducts(Pageable pageable) {
        log.info("Service: Fetching rejected products");
        return getProductsByStatus(ProductStatus.REJECTED, pageable);
    }

    @Override
    public Page<ProductListingDTO> getMyActiveProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching authenticated user's active products");
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ProductStatus.ACTIVE, pageable);
        log.info("Service: Found {} active products for user {}", productsPage.getTotalElements(), seller.getEmail());

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public Page<ProductListingDTO> getMyModerationProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching authenticated user's products in moderation");
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ProductStatus.MODERATION, pageable);
        log.info("Service: Found {} products in moderation for user {}", productsPage.getTotalElements(), seller.getEmail());

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public Page<ProductListingDTO> getMyRejectedProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching authenticated user's rejected products");
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ProductStatus.REJECTED, pageable);
        log.info("Service: Found {} rejected products for user {}", productsPage.getTotalElements(), seller.getEmail());

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public Map<ProductStatus, Long> getMyProductCounts(Authentication authentication) throws ResourceNotFoundException {
        log.info("Service: Getting product counts by status for authenticated user");
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Map<ProductStatus, Long> statusCounts = new EnumMap<>(ProductStatus.class);

        // Initialize all statuses with zero count
        for (ProductStatus status : ProductStatus.values()) {
            statusCounts.put(status, 0L);
        }

        // Get the counts from the repository
        List<Object[]> countResults = productRepository.countBySellerAndGroupByStatus(seller.getId());

        // Update the map with actual counts
        for (Object[] result : countResults) {
            ProductStatus status = (ProductStatus) result[0];
            Long count = (Long) result[1];
            statusCounts.put(status, count);
        }

        log.info("Service: Product counts by status retrieved for user {}: {}", seller.getEmail(), statusCounts);

        return statusCounts;
    }


    @Override
    public Page<ProductListingDTO> getMyDeactivatedProducts(Authentication authentication, Pageable pageable) throws ResourceNotFoundException {
        log.info("Service: Fetching authenticated user's deactivated products");
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ProductStatus.DEACTIVATED, pageable);
        log.info("Service: Found {} deactivated products for user {}", productsPage.getTotalElements(), seller.getEmail());

        return productsPage.map(productMapper::toProductListingDTO);
    }

    @Override
    public ProductResponseDTO deactivateProduct(UUID productId, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Deactivating product with ID: {}", productId);

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Check if the authenticated user is the owner of the product
        if (!product.getSeller().getId().equals(seller.getId())) {
            log.warn("Service: Unauthorized attempt to deactivate product with ID: {}", productId);
            throw new NotAuthorizedException("You are not authorized to deactivate this product");
        }

        // Change product status to DEACTIVATED
        product.setStatus(ProductStatus.DEACTIVATED);
        Product updatedProduct = productRepository.save(product);
        log.info("Service: Product deactivated with ID: {}", updatedProduct.getId());

        // Update the Elasticsearch index
        productIndexService.indexProduct(updatedProduct, 0);

        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public ProductResponseDTO activateProduct(UUID productId, Authentication authentication)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: activating product with ID: {}", productId);

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with ID : {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Check if the authenticated user is the owner of the product
        if (!product.getSeller().getId().equals(seller.getId())) {
            log.warn("Service: Unauthorized attempt to activate product with ID: {}", productId);
            throw new NotAuthorizedException("You are not authorized to deactivate this product");
        }

        // Change product status to DEACTIVATED
        product.setStatus(ProductStatus.ACTIVE);
        Product updatedProduct = productRepository.save(product);
        log.info("Service: Product activated with ID: {}", updatedProduct.getId());

        // Update the Elasticsearch index
        productIndexService.indexProduct(updatedProduct, 0);

        return productMapper.toDTO(updatedProduct);
    }

}
