package my.project.qri3a.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("api/v1/images")
public class ImageController {

    private final ImageService imageService;

    /**
     * GET /images/products/{productId}/images
     * Retrieve all images for a specific product.
     */
    @GetMapping("/p/{productId}")
    public ResponseEntity<ApiResponse<List<ImageResponseDTO>>> getImages(
            @PathVariable UUID productId
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching all images for product '{}'", productId);

        List<ImageResponseDTO> images = imageService.getImages(productId);

        ApiResponse<List<ImageResponseDTO>> response = new ApiResponse<>(images, "Images retrieved successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /images/products/{productId}/image
     * Upload a single image for a product.
     */
    @PostMapping("/p/{productId}")
    public ResponseEntity<ApiResponse<ImageResponseDTO>> uploadImage(
            @PathVariable UUID productId,
            @RequestParam("image") MultipartFile image
    ) throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Controller: Uploading image for product '{}'", productId);

        ImageResponseDTO  imageResponseDTO = imageService.uploadImage(productId, image);

        ApiResponse<ImageResponseDTO> response = new ApiResponse<>(imageResponseDTO, "Image uploaded successfully.", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /images/products/{productId}/image/{imageId}
     * Delete a specific image from a product.
     */
    @DeleteMapping("/products/{productId}/image/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId
    ) throws ResourceNotFoundException {
        log.info("Controller: Deleting image '{}' for product '{}'", imageId, productId);

        imageService.deleteImage(productId, imageId);

        ApiResponse<Void> response = new ApiResponse<>(null, "Image deleted successfully.", HttpStatus.NO_CONTENT.value());
        return ResponseEntity.noContent().build();
    }
}
