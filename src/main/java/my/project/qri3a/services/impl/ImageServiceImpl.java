package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.entities.Image;
import my.project.qri3a.entities.Product;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import my.project.qri3a.mappers.ImageMapper;
import my.project.qri3a.repositories.ImageRepository;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.services.ImageService;
import my.project.qri3a.services.S3Service;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageServiceImpl implements ImageService {

    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final S3Service s3Service;
    private final ImageMapper imageMapper;

    /**
     * Récupère toutes les images pour un produit donné.
     *
     * @param productId ID du produit
     * @return Liste de DTO représentant les images
     * @throws ResourceNotFoundException si le produit n'est pas trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public List<ImageResponseDTO> getImages(UUID productId) throws ResourceNotFoundException {
        log.info("Service: Fetching all images for product '{}'", productId);

        // Vérifier que le produit existe
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + productId));


        // Récupérer toutes les images associées au produit
        List<Image> images = imageRepository.findByProductId(productId);

        // Convertir les entités Image en DTO
        return images.stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ImageResponseDTO uploadImage(UUID productId, MultipartFile file) throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Service: Uploading image for product '{}'", productId);

        // Vérifier que le produit existe
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + productId));

        // Validation du fichier
        if (file.isEmpty()) {
            throw new ResourceNotValidException("File is empty.");
        }

        if (!isImageFile(file)) {
            throw new ResourceNotValidException("Only image files are allowed.");
        }

        // Vérifier le nombre d'images déjà associées au produit
        long imageCount = imageRepository.countByProductId(productId);
        if (imageCount >= 4) {
            throw new ResourceNotValidException("Maximum 4 images allowed per product.");
        }

        // Générer un nom unique pour le fichier
        String filename = generateUniqueFileName(file.getOriginalFilename());

        // Télécharger le fichier sur S3
        String fileUrl = s3Service.uploadFile(file, filename);

        log.info("Image uploaded successfully and the URL is: {}", fileUrl);

        // Créer une entité Image
        Image image = Image.builder()
                .url(fileUrl)
                .product(product)
                .build();

        // Ajouter l'image au produit
        product.addImage(image);

        // Sauvegarder le produit (cascade persistera l'image)
        Image savedImage = imageRepository.save(image);
        productRepository.save(product);

        log.info("Service: Image uploaded with URL: {}", fileUrl);

        // Convertir l'entité Image en DTO
        return imageMapper.toDTO(savedImage);
    }

    @Override
    @Transactional
    public void deleteImage(UUID productId, UUID imageId) throws ResourceNotFoundException {
        log.info("Service: Deleting image '{}' for product '{}'", imageId, productId);

        // Vérifier que le produit existe
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + productId));

        // Trouver l'image associée au produit
        Image image = imageRepository.findByProductIdAndId(productId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with ID " + imageId + " for product " + productId));

        // Extraire le nom du fichier depuis l'URL
        String imageUrl = image.getUrl();
        String filename = extractFileName(imageUrl);

        // Supprimer le fichier de S3
        s3Service.deleteFile(filename);

        // Supprimer l'image de la base de données
        product.removeImage(image);
        imageRepository.delete(image);
        productRepository.save(product);

        log.info("Service: Image with ID: {} deleted from product with ID: {}", imageId, productId);
    }

    @Override
    public List<ImageResponseDTO> uploadImages(UUID productId, List<MultipartFile> files) throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Service: Uploading {} images for product '{}'", files.size(), productId);

        // Vérifier que le produit existe
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + productId));

        // Validation des fichiers
        if (files.isEmpty()) {
            throw new ResourceNotValidException("No files provided.");
        }

        if (files.size() > 4) {
            throw new ResourceNotValidException("Cannot upload more than 4 images at once.");
        }

        // Vérifier le nombre d'images déjà associées au produit
        long existingImageCount = imageRepository.countByProductId(productId);
        if (existingImageCount + files.size() > 4) {
            throw new ResourceNotValidException("Uploading these images would exceed the maximum of 4 images per product.");
        }

        List<ImageResponseDTO> uploadedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ResourceNotValidException("One of the files is empty.");
            }

            if (!isImageFile(file)) {
                throw new ResourceNotValidException("Only image files are allowed.");
            }

            // Générer un nom unique pour le fichier
            String filename = generateUniqueFileName(file.getOriginalFilename());

            // Télécharger le fichier sur S3
            String fileUrl = s3Service.uploadFile(file, filename);
            log.info("Image uploaded successfully and the URL is: {}", fileUrl);

            // Créer une entité Image
            Image image = Image.builder()
                    .url(fileUrl)
                    .product(product)
                    .build();

            // Ajouter l'image au produit
            product.addImage(image);

            // Sauvegarder l'image
            Image savedImage = imageRepository.save(image);
            uploadedImages.add(imageMapper.toDTO(savedImage));
        }

        // Sauvegarder le produit avec les nouvelles images
        productRepository.save(product);
        log.info("Service: {} images uploaded successfully for product '{}'", uploadedImages.size(), productId);

        return uploadedImages;
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return (
                contentType.equals("image/png") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/gif")
        );
    }

    private String generateUniqueFileName(String originalFilename) {
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString() + fileExtension;
    }

    @Override
    @Transactional
    public void deleteImageById(UUID imageId) throws ResourceNotFoundException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with ID " + imageId));
        imageRepository.delete(image);
        log.info("ImageService: Deleted image with ID: {}", imageId);
    }

    private String extractFileName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }

}
