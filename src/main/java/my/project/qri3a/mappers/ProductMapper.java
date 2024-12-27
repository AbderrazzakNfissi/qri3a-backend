package my.project.qri3a.mappers;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.dtos.responses.UserDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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


    public Product toEntity(ProductRequestDTO productRequestDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productRequestDTO, product);
        return product;
    }

    public void updateEntityFromDTO(ProductRequestDTO dto, Product entity) {
        BeanUtils.copyProperties(dto, entity, "id", "createdAt", "updatedAt", "seller","images");
    }
}
