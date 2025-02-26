package my.project.qri3a.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import my.project.qri3a.services.ProductIndexService;
import my.project.qri3a.services.S3Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageServiceImpl implements ImageService {

    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final S3Service s3Service;
    private final ImageMapper imageMapper;
    private final ProductIndexService productIndexService;
 
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
        int order = 1;
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
                    .order(order)
                    .build();
            order++;
            // Ajouter l'image au produit
            product.addImage(image);

            // Sauvegarder l'image
            Image savedImage = imageRepository.save(image);
            uploadedImages.add(imageMapper.toDTO(savedImage));
        }

        // Sauvegarder le produit avec les nouvelles images
        productRepository.save(product);
        log.info("Service: {} images uploaded successfully for product '{}'", uploadedImages.size(), productId);
        
        this.productIndexService.indexProduct(product,files.size());
        return uploadedImages;
    }


    public List<ImageResponseDTO> updateImages(UUID productId, List<UUID> existingImageIds, List<MultipartFile> newFiles)
            throws ResourceNotFoundException, IOException, ResourceNotValidException {
        log.info("Service: Updating images for product '{}'", productId);

        // Création de listes sécurisées pour éviter les valeurs nulles
        final List<UUID> safeExistingImageIds = (existingImageIds == null) ? new ArrayList<>() : existingImageIds;
        final List<MultipartFile> safeNewFiles = (newFiles == null) ? new ArrayList<>() : newFiles;

        // Récupération du produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID " + productId));

        // Sélection des images à supprimer (celles dont l'ID n'est pas dans la liste des IDs à conserver)
        List<Image> imagesToDelete = product.getImages().stream()
                .filter(image -> !safeExistingImageIds.contains(image.getId()))
                .toList();

        int nbOfImages = product.getImages().size() - imagesToDelete.size() + safeNewFiles.size();
        log.info("==> images to delete: {}", imagesToDelete.size());
        log.info("==> number of images: {}", nbOfImages);

        // Suppression physique des images sur S3
        for (Image image : imagesToDelete) {
            String filename = extractFileName(image.getUrl());
            s3Service.deleteFile(filename);
            log.info("Deleted image with ID: {} and url {}", image.getId(), image.getUrl());
        }

        // Au lieu de remplacer la collection par une nouvelle instance,
        // on la modifie in situ pour respecter l'orphan removal
        List<Image> imagesToKeep = product.getImages().stream()
                .filter(image -> safeExistingImageIds.contains(image.getId()))
                .collect(Collectors.toList());
        product.getImages().clear();
        product.getImages().addAll(imagesToKeep);
        product.setImages(product.getImages());

        // Vérifier que l'ajout des nouvelles images ne dépasse pas le maximum autorisé (4)
        if (product.getImages().size() + safeNewFiles.size() > 4) {
            throw new ResourceNotValidException("Uploading these images would exceed the maximum of 4 images per product.");
        }

        // Upload des nouvelles images et ajout au produit
        for (MultipartFile file : safeNewFiles) {
            if (file.isEmpty()) {
                throw new ResourceNotValidException("One of the files is empty.");
            }
            if (!isImageFile(file)) {
                throw new ResourceNotValidException("Only image files are allowed.");
            }
            String uniqueFilename = generateUniqueFileName(file.getOriginalFilename());
            String fileUrl = s3Service.uploadFile(file, uniqueFilename);
            log.info("Image uploaded successfully and the URL is: {}", fileUrl);

            Image image = Image.builder()
                    .url(fileUrl)
                    .product(product)
                    .order(4)
                    .build();
            product.addImage(image);
            imageRepository.save(image);
        }

        List<Image> updatedImages = product.getImages();

        // Sauvegarde du produit mis à jour et re-indexation si nécessaire
        productRepository.save(product);
        productIndexService.indexProduct(product, nbOfImages);
        log.info("Service: Images updated for product '{}'", productId);

        // Retourne la liste complète des images mises à jour sous forme de DTO
        return updatedImages.stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());
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
