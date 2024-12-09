package my.project.qri3a.services;

import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ImageService {

     /**
      * Retrieve all images for a specific product.
      *
      * @param productId ID of the product
      * @return List of ImageResponseDTO representing all images of the product
      * @throws ResourceNotFoundException if the product is not found
      */
     List<ImageResponseDTO> getImages(UUID productId) throws ResourceNotFoundException;
     ImageResponseDTO uploadImage(UUID productId, MultipartFile file) throws ResourceNotFoundException, IOException, ResourceNotValidException;
     void deleteImage(UUID productId, UUID imageId) throws ResourceNotFoundException;
}
