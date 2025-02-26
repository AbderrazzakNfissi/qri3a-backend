package my.project.qri3a.services;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import my.project.qri3a.dtos.requests.ImageOrderDTO;
import org.springframework.web.multipart.MultipartFile;

import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;

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
     /**
      * Upload multiple images for a specific product.
      *
      * @param productId ID du produit
      * @param files Liste de fichiers Multipart à télécharger
      * @return Liste de ImageResponseDTO représentant les images téléchargées
      * @throws ResourceNotFoundException si le produit n'est pas trouvé
      * @throws ResourceNotValidException si les fichiers ne sont pas valides ou dépassent la limite
      * @throws IOException en cas d'erreur lors du téléchargement
      */
     List<ImageResponseDTO> uploadImages(UUID productId, List<MultipartFile> files) throws ResourceNotFoundException, IOException, ResourceNotValidException;
     void deleteImageById(UUID imageId) throws ResourceNotFoundException;
     List<ImageResponseDTO> updateImages(UUID productId, List<UUID> existingImageIds, List<MultipartFile> newFiles, List<ImageOrderDTO> imagesOrder)
        throws ResourceNotFoundException, IOException, ResourceNotValidException;
}
