package my.project.qri3a.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import my.project.qri3a.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.ProductSuggestionDTO;
import my.project.qri3a.exceptions.NotAuthorizedException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ProductService;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/v1/products
     * Example: /api/v1/products?page=0&size=10&sort=price,asc&category=SMARTPHONES_AND_TELEPHONES&location=Rabat&condition=NEW
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getAllProducts(
            Pageable pageable,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String city
    ) throws ResourceNotValidException {
        log.info("Controller: Fetching all products with pagination: page={}, size={}, sort={}, category={}, location={}, condition={}, sellerId={}, minPrice={}, maxPrice={}, city={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), category, location, condition, sellerId, minPrice, maxPrice,city);

        Page<ProductListingDTO> productsPage = productService.getAllProducts(pageable, category, location, condition, sellerId, minPrice, maxPrice,city);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(productsPage, "Products fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }



    /**
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable UUID id) throws ResourceNotFoundException {
        log.info("Controller: Fetching product with ID: {}", id);
        ProductResponseDTO productResponseDTO = productService.getProductById(id);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(productResponseDTO, "Product fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getMyProducts(
            Pageable pageable,
            Authentication authentication
    ) throws ResourceNotValidException, ResourceNotFoundException {
        Page<ProductListingDTO> myProducts = productService.getMyProducts(authentication, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(myProducts, "Products fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(@Valid @RequestBody ProductRequestDTO productRequestDTO,Authentication authentication)
            throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Controller: Creating new product with title: {}", productRequestDTO.getTitle());
        ProductResponseDTO createdProduct = productService.createProduct(productRequestDTO,authentication);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(createdProduct, "Product created successfully.", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(@PathVariable UUID id,
                                                                         @Valid @RequestBody ProductRequestDTO productRequestDTO)
            throws ResourceNotFoundException, ResourceNotValidException {
        log.info("Controller: Updating product with ID: {}", id);
        ProductResponseDTO updatedProduct = productService.updateProduct(id, productRequestDTO);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(updatedProduct, "Product updated successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) throws ResourceNotFoundException {
        log.info("Controller: Deleting product with ID: {}", id);
        productService.deleteProduct(id);
        ApiResponse<Void> response = new ApiResponse<>(null, "Product deleted successfully.", HttpStatus.NO_CONTENT.value());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/my/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyProduct(
            @PathVariable UUID id,
            Authentication authentication) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Deleting my product with ID: {}", id);
        productService.deleteMyProduct(id, authentication);
        ApiResponse<Void> response = new ApiResponse<>(null, "Product deleted successfully.", HttpStatus.NO_CONTENT.value());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/recommended")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getRecommendedProducts(
            @PathVariable UUID id,
            Pageable pageable
    ) throws ResourceNotFoundException {
        log.info("Controller: Récupération des produits recommandés pour le produit ID: {}", id);
        Page<ProductListingDTO> recommendedProducts = productService.getRecommendedProducts(id, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                recommendedProducts,
                "Produits recommandés récupérés avec succès.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search-suggestions")
    public ResponseEntity<ApiResponse<List<ProductListingDTO>>> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Controller: Récupération des suggestions de recherche pour le terme: {}", query);
        List<ProductListingDTO> suggestions = productService.searchProductSuggestions(query, limit);
        ApiResponse<List<ProductListingDTO>> response = new ApiResponse<>(
                suggestions,
                "Suggestions de recherche récupérées avec succès.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /*
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        log.info("Controller: Recherche de produits avec le terme: {}", query);

        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ProductListingDTO> results = productService.searchProducts(query, pageable);

        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                results,
                "Résultats de recherche récupérés avec succès.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }
  */

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getProductsByUserId(
            @PathVariable UUID userId,
            Pageable pageable
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching products for user ID: {} with pagination: page={}, size={}, sort={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getProductsByUserId(userId, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Products fetched successfully for user ID " + userId + ".",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String city
    ) {
        log.info("Controller: Recherche de produits avec le terme: {} et filtres - category: {}, location: {}, condition: {}, minPrice: {}, maxPrice: {}, city: {}",
                query, category, location, condition, minPrice, maxPrice, city);

        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Appeler la méthode de recherche avec filtres
        Page<ProductListingDTO> results = productService.searchProducts(query, pageable, category, location, condition, minPrice, maxPrice, city);

        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                results,
                "Résultats de recherche récupérés avec succès.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search-elastic")
    public ResponseEntity<ApiResponse<Page<ProductDoc>>> searchProductsElastic(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String city) {

        log.info("Controller: Elasticsearch search with query: {} and filters - category: {}, location: {}, condition: {}, minPrice: {}, maxPrice: {}, city: {}",
                query, category, location, condition, minPrice, maxPrice, city);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDoc> results = productService.searchProductsElastic(query, pageable, category, location, condition, minPrice, maxPrice, city);
        ApiResponse<Page<ProductDoc>> response = new ApiResponse<>(results, "Elasticsearch search results fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/find-all")
    public ResponseEntity<ApiResponse<Page<ProductDoc>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDoc> results = productService.findAll(pageable);
        ApiResponse<Page<ProductDoc>> response = new ApiResponse<>(
                results,
                "Résultats de recherche récupérés avec succès.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<ProductSuggestionDTO>>> getSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Controller: Récupération des suggestions de recherche pour le terme: {} using Elastic Search", query);
        List<ProductDoc> suggestions = productService.searchProductSuggestionsElastic(query);

        List<ProductSuggestionDTO> suggestionDTOs = suggestions.stream()
                .limit(limit)
                .map(doc -> {
                    ProductSuggestionDTO dto = new ProductSuggestionDTO();
                    dto.setId(doc.getId());
                    dto.setTitle(doc.getTitle());
                    return dto;
                })
                .collect(Collectors.toList());

        ApiResponse<List<ProductSuggestionDTO>> response = new ApiResponse<>(
                suggestionDTOs,
                "Suggestions de recherche récupérées avec succès.",
                HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }


    /**
     * Approve a product (change status to ACTIVE)
     * POST /api/v1/products/{id}/approve
     */
    @PostMapping("/{id}/approve")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> approveProduct(@PathVariable UUID id)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Approving product with ID: {}", id);
        ProductResponseDTO approvedProduct = productService.approveProduct(id);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                approvedProduct,
                "Product approved successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a product (change status to REJECTED)
     * POST /api/v1/products/{id}/reject
     */
    @PostMapping("/{id}/reject")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> rejectProduct(@PathVariable UUID id)
            throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Rejecting product with ID: {}", id);
        ProductResponseDTO rejectedProduct = productService.rejectProduct(id);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                rejectedProduct,
                "Product rejected successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a product (change status from DEACTIVATED to ACTIVE)
     * POST /api/v1/products/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> activateProduct(
            @PathVariable UUID id,
            Authentication authentication
    ) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Activating product with ID: {}", id);

        ProductResponseDTO activatedProduct = productService.activateProduct(id, authentication);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                activatedProduct,
                "Product activated successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    /**
     * Deactivate a product (change status to DEACTIVATED)
     * POST /api/v1/products/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> deactivateProduct(
            @PathVariable UUID id,
            Authentication authentication
    ) throws ResourceNotFoundException, NotAuthorizedException {
        log.info("Controller: Deactivating product with ID: {}", id);

        ProductResponseDTO deactivatedProduct = productService.deactivateProduct(id, authentication);
        ApiResponse<ProductResponseDTO> response = new ApiResponse<>(
                deactivatedProduct,
                "Product deactivated successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    /**
     * Get products with ACTIVE status
     * GET /api/v1/products/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getActiveProducts(Pageable pageable) {
        log.info("Controller: Fetching active products with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getActiveProducts(pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Active products fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get products with MODERATION status
     * GET /api/v1/products/moderation
     */
    @GetMapping("/moderation")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getModerationProducts(Pageable pageable) {
        log.info("Controller: Fetching products in moderation with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getModerationProducts(pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Products in moderation fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get products with REJECTED status
     * GET /api/v1/products/rejected
     */
    @GetMapping("/rejected")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getRejectedProducts(Pageable pageable) {
        log.info("Controller: Fetching rejected products with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getRejectedProducts(pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Rejected products fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated user's products with ACTIVE status
     * GET /api/v1/products/my/active
     */
    @GetMapping("/my/active")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getMyActiveProducts(
            Pageable pageable,
            Authentication authentication
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching authenticated user's active products with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getMyActiveProducts(authentication, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Your active products fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated user's products with MODERATION status
     * GET /api/v1/products/my/moderation
     */
    @GetMapping("/my/moderation")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getMyModerationProducts(
            Pageable pageable,
            Authentication authentication
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching authenticated user's products in moderation with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getMyModerationProducts(authentication, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Your products in moderation fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated user's products with REJECTED status
     * GET /api/v1/products/my/rejected
     */
    @GetMapping("/my/rejected")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getMyRejectedProducts(
            Pageable pageable,
            Authentication authentication
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching authenticated user's rejected products with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getMyRejectedProducts(authentication, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Your rejected products fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    /**
     * Get counts of products by status for the authenticated user
     * GET /api/v1/products/my/counts
     */
    @GetMapping("/my/counts")
    public ResponseEntity<ApiResponse<Map<ProductStatus, Long>>> getMyProductCounts(
            Authentication authentication) throws ResourceNotFoundException {
        log.info("Controller: Fetching product counts by status for authenticated user");

        Map<ProductStatus, Long> statusCounts = productService.getMyProductCounts(authentication);

        ApiResponse<Map<ProductStatus, Long>> response = new ApiResponse<>(
                statusCounts,
                "Product counts fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated user's products with DEACTIVATED status
     * GET /api/v1/products/my/deactivated
     */
    @GetMapping("/my/deactivated")
    public ResponseEntity<ApiResponse<Page<ProductListingDTO>>> getMyDeactivatedProducts(
            Pageable pageable,
            Authentication authentication
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching authenticated user's deactivated products with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ProductListingDTO> productsPage = productService.getMyDeactivatedProducts(authentication, pageable);
        ApiResponse<Page<ProductListingDTO>> response = new ApiResponse<>(
                productsPage,
                "Your deactivated products fetched successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }





}
