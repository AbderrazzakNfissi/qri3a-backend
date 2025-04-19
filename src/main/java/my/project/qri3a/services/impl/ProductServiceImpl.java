package my.project.qri3a.services.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import my.project.qri3a.entities.*;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.repositories.UserPreferenceRepository;
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
    private final NotificationService notificationService;
    private final UserPreferenceRepository userPreferenceRepository;

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

    /**
     * Récupère un produit par son slug SEO-friendly
     * 
     * @param slug Le slug du produit à récupérer
     * @param authentication Les informations d'authentification de l'utilisateur
     * @return Les détails du produit
     * @throws ResourceNotFoundException si le produit n'est pas trouvé
     */
    @Override
    public ProductResponseDTO getProductBySlug(String slug, Authentication authentication) throws ResourceNotFoundException {
        log.info("Service: Fetching product with slug: {}", slug);

        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> {
                    log.warn("Service: Product not found with slug: {}", slug);
                    return new ResourceNotFoundException("Product not found with slug " + slug);
                });

        // Si le produit n'est pas actif, vérifier si l'authentification existe
        if (product.getStatus() != ProductStatus.ACTIVE) {
            // Si l'utilisateur n'est pas authentifié, on rejette l'accès
            if (authentication == null) {
                log.warn("Service: Unauthenticated access attempt to non-active product with slug: {}", slug);
                throw new NotAuthorizedException("You must be logged in to view this product");
            }

            // Obtenir l'utilisateur authentifié
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            // Vérifier si l'utilisateur est le propriétaire du produit
            if (!product.getSeller().getId().equals(currentUser.getId())) {
                log.warn("Service: Unauthorized access to non-active product with slug: {}", slug);
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

        // Gestion des options de livraison en fonction de la catégorie
        if (!isCategorySupportsDelivery(product.getCategory())) {
            // Forcer NO pour la livraison et réinitialiser les autres options pour les catégories non livrables
            product.setDelivery("NO");
            product.setDeliveryFee(null);
            product.setDeliveryAllMorocco(false);
            product.setDeliveryZones(null);
            product.setDeliveryTime(null);
        }

        // Enregistrer le produit
        Product savedProduct = productRepository.save(product);
        log.info("Service: Product created with ID: {}", savedProduct.getId());
        
        // Ajouter l'indexation pour le SEO
        productIndexService.indexProduct(savedProduct, savedProduct.getImages().size());
        log.info("Service: Product indexed for SEO with ID: {}", savedProduct.getId());

        return productMapper.toDTO(savedProduct);
    }

    @Override
    public ProductResponseDTO updateMyProduct(UUID productId, ProductRequestDTO productRequestDTO, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Updating my product with ID: {}", productId);

        // Récupérer l'utilisateur à partir de l'authentification
        String email = authentication.getName();
        User currentUser = userService.getUserByEmail(email);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service : Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Vérifier que l'utilisateur actuel est le propriétaire du produit
        if (!existingProduct.getSeller().getId().equals(currentUser.getId())) {
            log.warn("Service: User {} is not authorized to update product {}", currentUser.getId(), productId);
            throw new NotAuthorizedException("You are not authorized to update this product");
        }

        // Mettre à jour les champs de l'entité à partir du DTO
        productMapper.updateEntityFromDTO(productRequestDTO, existingProduct);

        // Vérifier et mettre à jour les options de livraison en fonction de la catégorie
        if (!isCategorySupportsDelivery(existingProduct.getCategory())) {
            existingProduct.setDelivery("NO");
            existingProduct.setDeliveryFee(null);
            existingProduct.setDeliveryAllMorocco(false);
            existingProduct.setDeliveryZones(null);
            existingProduct.setDeliveryTime(null);
        }

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
                    log.warn("Service : Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });
        // Delete all associated images from S3
        for (Image image : product.getImages()) {
            String filename = image.getUrl().substring(image.getUrl().lastIndexOf('/') + 1);
            s3Service.deleteFile(filename);
        }
        productRepository.delete(product);
        log.info("Service : Product deleted with ID: {}", productId);
        productIndexService.deleteProductIndex(productId);
    }


    @Override
    public Page<ProductListingDTO> getMyProducts(Authentication authentication, Pageable pageable) throws ResourceNotValidException {
        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);
        log.info("=> seller email: {}", seller.getEmail());
        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ProductStatus.ACTIVE, pageable);
        return productsPage.map(productMapper::toProductListingDTO);
    }


    @Transactional
    @Override
    public void deleteMyProduct(UUID productId, Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Deleting my product with ID: {}", productId);

        String email = authentication.getName();
        User seller = userService.getUserByEmail(email);


        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service : Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });


        if (!product.getSeller().getId().equals(seller.getId())) {
            log.warn("Service: Unauthorized attempt to delete product with ID: {}", productId);
            throw new NotAuthorizedException("You are not authorized to delete this product");
        }

        productRepository.removeProductFromAllWishlists(productId);
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
        Page<Product> productsPage = productRepository.findBySellerAndStatusOrderByCreatedAtDesc(user, ProductStatus.ACTIVE, pageable);
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
    public List<ProductDoc> searchProductSuggestionsElastic(String query, String category, int limit) {
        log.info("Service: Recherche des suggestions de produits pour le terme: {} - Using Elastic Search", query);

        // Validation de l'entrée
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Limiter le nombre de résultats
        int maxResults = Math.min(limit, 10);

        // Rechercher les produits via Elasticsearch avec la catégorie spécifiée
        List<ProductDoc> productDocs = productDocRepository.findTop10ByTitleOrDescription(query, category);
        return productDocs;
    }

    @Override
    public ProductResponseDTO approveProduct(UUID productId) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Service: Approving product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Service  : Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID " + productId);
                });

        // Change product status to ACTIVE
        product.setStatus(ProductStatus.ACTIVE);
        Product updatedProduct = productRepository.save(product);
        log.info("Service: Product approved with ID: {}", updatedProduct.getId());

        // Update the Elasticsearch index
        productIndexService.indexProduct(updatedProduct, 0);

        notifyUser(updatedProduct);

        // Notifier les utilisateurs intéressés
        productMatchingService.notifyInterestedUsers(updatedProduct);

        return productMapper.toDTO(updatedProduct);
    }

    private void notifyUser(Product product) {
        log.info("Service: Notifying seller about product approval, product ID: {}", product.getId());

        User seller = product.getSeller();
        if (seller == null) {
            log.warn("Service: Cannot notify seller, seller not found for product ID: {}", product.getId());
            return;
        }

        try {
            // Récupérer la préférence de langue de l'utilisateur
            String userLang = getUserLanguagePreference(seller);

            // Construire le message de notification en fonction de la langue
            String notificationMessage;

            switch (userLang) {
                case "en":
                    notificationMessage = String.format(
                            "Your listing \"%s\" has been approved and is now visible to all users.",
                            product.getTitle());
                    break;
                case "arm":
                    notificationMessage = String.format(
                            "تم الموافقة على إعلانك \"%s\" وهو الآن مرئي لجميع المستخدمين.",
                            product.getTitle());
                    break;
                case "fr":
                default:
                    notificationMessage = String.format(
                            "Votre annonce \"%s\" a été approuvée et est maintenant visible par tous les utilisateurs.",
                            product.getTitle());
                    break;
            }

            // Créer une nouvelle notification
            Notification notification = Notification.builder()
                    .user(seller)
                    .product(product)
                    .category(product.getCategory())
                    .body(notificationMessage)
                    .read(false)
                    .build();

            // Enregistrer et envoyer la notification
            notificationService.createNotification(notification);

            log.info("Service: Notification sent to seller ID: {} for approved product ID: {} in language: {}",
                    seller.getId(), product.getId(), userLang);
        } catch (Exception e) {
            log.error("Service: Error while sending notification to seller ID: {} for product ID: {}: {}",
                    seller.getId(), product.getId(), e.getMessage());
        }
    }

    /**
     * Récupère la préférence de langue de l'utilisateur, ou retourne la valeur par défaut (fr)
     */
    private String getUserLanguagePreference(User user) {
        try {
            Optional<UserPreference> langPref = userPreferenceRepository.findByUserIdAndKey(user.getId(), "lang");
            return langPref.map(UserPreference::getValue).orElse("fr"); // français par défaut
        } catch (Exception e) {
            log.warn("Service: Error retrieving language preference for user ID: {}, defaulting to French", user.getId());
            return "fr";
        }
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

    @Override
    public long getProductCountByStatus(ProductStatus status) {
        log.info("Service: Getting count of products with status: {}", status);
        return productRepository.countByStatus(status);
    }

    @Override
    public Map<ProductStatus, Long> getAllProductCounts() {
        log.info("Service: Getting counts of products by status");

        Map<ProductStatus, Long> statusCounts = new EnumMap<>(ProductStatus.class);

        // Initialize all statuses with zero count
        for (ProductStatus status : ProductStatus.values()) {
            statusCounts.put(status, 0L);
        }

        // Get the counts from the repository
        List<Object[]> countResults = productRepository.countGroupByStatus();

        // Update the map with actual counts
        for (Object[] result : countResults) {
            ProductStatus status = (ProductStatus) result[0];
            Long count = (Long) result[1];
            statusCounts.put(status, count);
        }

        log.info("Service: Product counts by status: {}", statusCounts);

        return statusCounts;
    }


    private boolean isCategorySupportsDelivery(ProductCategory category) {
        // Liste des catégories qui ne supportent PAS la livraison
        List<ProductCategory> nonDeliverableCategories = Arrays.asList(
                ProductCategory.CARS,
                ProductCategory.MOTORCYCLES,
                ProductCategory.BICYCLES,
                ProductCategory.VEHICLE_PARTS,
                ProductCategory.TRUCKS_AND_MACHINERY,
                ProductCategory.BOATS,
                ProductCategory.OTHER_VEHICLES,
                ProductCategory.REAL_ESTATE_SALES,
                ProductCategory.APARTMENTS_FOR_SALE,
                ProductCategory.HOUSES_FOR_SALE,
                ProductCategory.VILLAS_RIADS_FOR_SALE,
                ProductCategory.OFFICES_FOR_SALE,
                ProductCategory.COMMERCIAL_SPACES_FOR_SALE,
                ProductCategory.LAND_AND_FARMS_FOR_SALE,
                ProductCategory.OTHER_REAL_ESTATE_FOR_SALE,
                ProductCategory.REAL_ESTATE_RENTALS,
                ProductCategory.APARTMENTS_FOR_RENT,
                ProductCategory.HOUSES_FOR_RENT,
                ProductCategory.VILLAS_RIADS_FOR_RENT,
                ProductCategory.OFFICES_FOR_RENT,
                ProductCategory.COMMERCIAL_SPACES_FOR_RENT,
                ProductCategory.LAND_AND_FARMS_FOR_RENT,
                ProductCategory.OTHER_REAL_ESTATE_FOR_RENT
        );

        return !nonDeliverableCategories.contains(category);
    }

    @Override
    public Page<ProductListingDTO> getProductsByMainCategory(String mainCategory, Pageable pageable) {
        log.info("Service: Fetching products for main category: {}", mainCategory);

        try {
            // Valider que la catégorie principale existe
            ProductCategory mainCat = ProductCategory.valueOf(mainCategory.toUpperCase());

            // Obtenir la liste des sous-catégories pour cette catégorie principale
            List<ProductCategory> subCategories = getSubcategoriesForMainCategory(mainCat);

            // Si aucune sous-catégorie n'est trouvée, retourner une page vide
            if (subCategories.isEmpty()) {
                return Page.empty(pageable);
            }

            // Création d'une spécification pour filtrer par statut ACTIVE et par les sous-catégories
            Specification<Product> spec = ProductSpecifications.hasStatus(ProductStatus.ACTIVE);

            // Ajouter les sous-catégories à la spécification avec des OR
            Specification<Product> categorySpec = null;
            for (ProductCategory category : subCategories) {
                if (categorySpec == null) {
                    categorySpec = ProductSpecifications.hasCategory(category);
                } else {
                    categorySpec = categorySpec.or(ProductSpecifications.hasCategory(category));
                }
            }

            // Combiner les spécifications
            if (categorySpec != null) {
                spec = spec.and(categorySpec);
            }

            // Exécuter la requête avec la spécification
            Page<Product> productsPage = productRepository.findAll(spec, pageable);
            log.info("Service: Found {} products for main category {}", productsPage.getTotalElements(), mainCategory);

            return productsPage.map(productMapper::toProductListingDTO);
        } catch (IllegalArgumentException e) {
            log.error("Service: Invalid main category: {}", mainCategory);
            throw new ResourceNotValidException("Invalid main category: " + mainCategory);
        }
    }

    /**
     * Renvoie la liste des sous-catégories pour une catégorie principale donnée
     * @param mainCategory La catégorie principale
     * @return Liste des sous-catégories
     */
    private List<ProductCategory> getSubcategoriesForMainCategory(ProductCategory mainCategory) {
        List<ProductCategory> allCategories = Arrays.asList(ProductCategory.values());

        switch (mainCategory) {
            case MARKET:
                return Arrays.asList(
                        ProductCategory.SMARTPHONES_AND_TELEPHONES,
                        ProductCategory.TABLETS_AND_E_BOOKS,
                        ProductCategory.LAPTOPS,
                        ProductCategory.DESKTOP_COMPUTERS,
                        ProductCategory.TELEVISIONS,
                        ProductCategory.ELECTRO_MENAGE,
                        ProductCategory.ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS,
                        ProductCategory.SMARTWATCHES_AND_ACCESSORIES,
                        ProductCategory.AUDIO_AND_HIFI,
                        ProductCategory.COMPUTER_COMPONENTS,
                        ProductCategory.STORAGE_AND_PERIPHERALS,
                        ProductCategory.PRINTERS_AND_SCANNERS,
                        ProductCategory.DRONES_AND_ACCESSORIES,
                        ProductCategory.NETWORK_EQUIPMENT,
                        ProductCategory.SMART_HOME_DEVICES,
                        ProductCategory.GAMING_ACCESSORIES,
                        ProductCategory.PHOTO_AND_VIDEO_EQUIPMENT,
                        ProductCategory.OTHER_CATEGORIES
                );
            case VEHICLES:
                return Arrays.asList(
                        ProductCategory.CARS,
                        ProductCategory.MOTORCYCLES,
                        ProductCategory.BICYCLES,
                        ProductCategory.VEHICLE_PARTS,
                        ProductCategory.TRUCKS_AND_MACHINERY,
                        ProductCategory.BOATS,
                        ProductCategory.OTHER_VEHICLES
                );
            case REAL_ESTATE:
                return Arrays.asList(
                        ProductCategory.REAL_ESTATE_SALES,
                        ProductCategory.APARTMENTS_FOR_SALE,
                        ProductCategory.HOUSES_FOR_SALE,
                        ProductCategory.VILLAS_RIADS_FOR_SALE,
                        ProductCategory.OFFICES_FOR_SALE,
                        ProductCategory.COMMERCIAL_SPACES_FOR_SALE,
                        ProductCategory.LAND_AND_FARMS_FOR_SALE,
                        ProductCategory.OTHER_REAL_ESTATE_FOR_SALE,
                        ProductCategory.REAL_ESTATE_RENTALS,
                        ProductCategory.APARTMENTS_FOR_RENT,
                        ProductCategory.HOUSES_FOR_RENT,
                        ProductCategory.VILLAS_RIADS_FOR_RENT,
                        ProductCategory.OFFICES_FOR_RENT,
                        ProductCategory.COMMERCIAL_SPACES_FOR_RENT,
                        ProductCategory.LAND_AND_FARMS_FOR_RENT,
                        ProductCategory.OTHER_REAL_ESTATE_FOR_RENT
                );
            default:
                return Collections.emptyList();
        }
    }


}
