package my.project.qri3a.controllers;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import my.project.qri3a.dtos.requests.ImageOrderDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ImageService;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("api/v1/images")
public class ImageController {

    private final ImageService imageService;


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

    @PostMapping("/p/{productId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponseDTO>>> uploadImages(
            @PathVariable UUID productId,
            @RequestParam("images") List<MultipartFile> images
    ) throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Controller: Uploading {} images for product '{}'", images.size(), productId);

        List<ImageResponseDTO> uploadedImages = imageService.uploadImages(productId, images);

        ApiResponse<List<ImageResponseDTO>> response = new ApiResponse<>(
                uploadedImages,
                "Images uploaded successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


    // Add this new endpoint to the ImageController

    @PutMapping("/p/{productId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponseDTO>>> updateImages(
            @PathVariable UUID productId,
            @RequestParam(value = "existingImageIds", required = false) List<UUID> existingImageIds,
            @RequestParam(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "imagesOrder", required = false) List<ImageOrderDTO> imagesOrder
    ) throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Controller: Updating images for product '{}'", productId);

        List<ImageResponseDTO> updatedImages = imageService.updateImages(productId, existingImageIds, newImages, imagesOrder);

        ApiResponse<List<ImageResponseDTO>> response = new ApiResponse<>(
                updatedImages,
                "Images updated successfully.",
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }


}
