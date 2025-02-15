package my.project.qri3a.services;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.entities.Product;
import my.project.qri3a.mappers.ProductMapper;
import my.project.qri3a.repositories.search.ProductDocRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ProductDocRepository productDocRepository;
    private final ProductMapper productMapper;

    public void indexProduct(Product product) {
        ProductDoc doc = productMapper.toProductDoc(product);
        productDocRepository.save(doc);
    }

    public void deleteProductIndex(UUID productId) {
        productDocRepository.deleteById(productId);
    }
}
