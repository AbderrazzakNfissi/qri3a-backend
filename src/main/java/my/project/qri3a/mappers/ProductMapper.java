package my.project.qri3a.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.dtos.responses.ProductListingDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.UserDTO;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductMapper {
    private final ImageMapper imageMapper;
    private final UserMapper userMapper;

    public ProductResponseDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponseDTO dto = new ProductResponseDTO();

        // Copy basic properties
        BeanUtils.copyProperties(product, dto, "seller", "images","createdAt");

        // Map seller information
        User seller = product.getSeller();
        UserDTO userDTO = userMapper.toUserDTO(seller);
        if (userDTO != null) {
            dto.setUser(userDTO);
        }

        // Map images
        List<ImageResponseDTO> imageDTOs = product.getImages().stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());
        dto.setImages(imageDTOs);

        LocalDateTime createdAt = product.getCreatedAt();
        if (createdAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }

    public ProductListingDTO toProductListingDTO(Product product) {
        if (product == null) {
            return null;
        }

        ProductListingDTO dto = new ProductListingDTO();

        // Copy basic properties, excluding "seller", "images", and "createdAt"
        BeanUtils.copyProperties(product, dto, "seller", "images", "createdAt");

        // Map images from Product to ImageResponseDTO
        List<ImageResponseDTO> imageDTOs = product.getImages().stream()
                .map(imageMapper::toDTO)
                .toList();

        // Check if the imageDTOs list is not empty before setting the first image
        if (!imageDTOs.isEmpty()) {
            dto.setImage(imageDTOs.get(0));
            dto.setNumberOfImages(imageDTOs.size());
        } else {
            // Optionally, handle the case when there are no images
            // For example, you can set a default image or leave it null
            dto.setImage(null); // or dto.setImage(defaultImageDTO);
        }

        // Handle the creation date
        LocalDateTime createdAt = product.getCreatedAt();
        if (createdAt != null) {
            // Convert LocalDateTime to ZonedDateTime with UTC timezone
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            // Define ISO 8601 format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Apply format and set in DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }

    /*
    public ProductListingDTO toProductListingDTO(ProductDoc doc) {
        if (doc == null) {
            return null;
        }
        ProductListingDTO dto = new ProductListingDTO();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setPrice(doc.getPrice());
        dto.setLocation(doc.getLocation());
        dto.setCity(doc.getCity());
        // Convertir la chaîne en enum (assurez-vous que la casse est correcte)
        dto.setCategory(ProductCategory.valueOf(doc.getCategory()));
        dto.setCondition(ProductCondition.valueOf(doc.getCondition()));
        // Les informations sur les images ne sont pas présentes dans le document Elasticsearch
        dto.setNumberOfImages(0);
        dto.setImage(null);

        return dto;
    }
  */


    public Product toEntity(ProductRequestDTO productRequestDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productRequestDTO, product);
        return product;
    }

    public void updateEntityFromDTO(ProductRequestDTO dto, Product entity) {
        BeanUtils.copyProperties(dto, entity, "id", "createdAt", "updatedAt", "seller","images");
    }


    public ProductDoc toProductDoc(Product product, int nbOfImages) {

        LocalDateTime createdAt = product.getCreatedAt();
        String createdAtStr = "";
        if (createdAt != null) {
            // Convert LocalDateTime to ZonedDateTime with UTC timezone
            ZonedDateTime utcDateTime = createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            // Define ISO 8601 format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Apply format and set in DTO
            createdAtStr = utcDateTime.format(formatter);
        }
        // Map images from Product to ImageResponseDTO
        List<ImageResponseDTO> imageDTOs = product.getImages().stream()
                .map(imageMapper::toDTO)
                .toList();

        return ProductDoc.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .location(product.getLocation())
                .city(product.getCity())
                .category(product.getCategory().name())
                .condition(product.getCondition().name())
                .createdAt(createdAtStr)
                .firstImageUrl(product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl())
                .numberOfImages(nbOfImages == 0 ? product.getImages().size() : nbOfImages)
                .build();
    }


}
