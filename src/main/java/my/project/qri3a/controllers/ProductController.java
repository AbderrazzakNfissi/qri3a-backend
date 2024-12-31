package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

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


}
