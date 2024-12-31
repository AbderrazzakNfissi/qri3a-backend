package my.project.qri3a.controllers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/favorites")
@Slf4j
public class FavoritesController {
    private final UserService userService;
    private final UserMapper userMapper;


    /**
     * POST /api/v1/favorites/{productId}
     * Ajouter un produit à la wishlist de l'utilisateur
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> addProductToWishlist(@PathVariable UUID productId, Authentication authentication) {
        String email = authentication.getName();
        UUID userId = userService.getUserByEmail(email).getId();
        log.info("Controller: Adding product with ID {} to wishlist of user with email {}", productId, email);
        userService.addProductToWishlist(userId, productId);
        ApiResponse<String> response = new ApiResponse<>("Product added to wishlist successfully.", "Product added to wishlist successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/users/wishlist/{productId}
     * Retirer un produit de la wishlist de l'utilisateur
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> removeProductFromWishlist(@PathVariable UUID productId,  Authentication authentication) {
        String email = authentication.getName();
        UUID userId = userService.getUserByEmail(email).getId();
        log.info("Controller: Removing product with ID {} from wishlist of user with email {}", productId, email);

        userService.removeProductFromWishlist(userId, productId);
        ApiResponse<String> response = new ApiResponse<>("Product removed from wishlist successfully.", "Product removed from wishlist successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);

    }


    /**
     * DELETE /api/v1/users/wishlist
     * Supprimer tous les produits de la wishlist de l'utilisateur
     */
    @DeleteMapping()
    public ResponseEntity<ApiResponse<String>> clearWishlist( Authentication authentication) {
        String email = authentication.getName();
        UUID userId = userService.getUserByEmail(email).getId();
        log.info("Controller: Clearing wishlist for user with  email {}", email);
        try {
            userService.clearWishlist(userId);
            ApiResponse<String> response = new ApiResponse<>("Wishlist cleared successfully.", "All products removed from wishlist.", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            log.error("Error clearing wishlist: {}", ex.getMessage());
            ApiResponse<String> errorResponse = new ApiResponse<>(null, ex.getMessage(), HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    //TO DO : I have to change the return type of getWishList ... to ProductInWithListDTO title, location...
    @GetMapping()
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getWishlist(Authentication authentication) {
        String email = authentication.getName();
        UUID userId = userService.getUserByEmail(email).getId();
        log.info("Controller: Fetching wishlist for user with email {}", email);
        List<ProductResponseDTO> wishlistProducts = userService.getWishlist(userId);
        ApiResponse<List<ProductResponseDTO>> response = new ApiResponse<>(wishlistProducts, "Wishlist fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getWishlistProductIds(Authentication authentication) {
        String email = authentication.getName();
        UUID userId = userService.getUserByEmail(email).getId();
        log.info("Controller: Fetching wishlist product IDs for user with email {}", email);
        List<UUID> wishlistProductIds = userService.getWishlistProductIds(userId);
        ApiResponse<List<UUID>> response = new ApiResponse<>(wishlistProductIds, "Wishlist product IDs fetched successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }
}