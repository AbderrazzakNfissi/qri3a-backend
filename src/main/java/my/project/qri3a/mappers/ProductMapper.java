package my.project.qri3a.mappers;

import my.project.qri3a.dtos.requests.ProductRequestDTO;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.entities.Product;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponseDTO toDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        BeanUtils.copyProperties(product, dto);
        if (product.getSeller() != null) {
            dto.setSellerId(product.getSeller().getId());
            dto.setSellerName(product.getSeller().getName());
        }
        return dto;
    }

    public Product toEntity(ProductRequestDTO productRequestDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productRequestDTO, product);
        return product;
    }

    public void updateEntityFromDTO(ProductRequestDTO dto, Product entity) {
        BeanUtils.copyProperties(dto, entity, "id", "createdAt", "updatedAt", "seller");
    }
}
