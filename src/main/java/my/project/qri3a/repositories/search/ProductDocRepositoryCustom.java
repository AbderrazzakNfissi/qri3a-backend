package my.project.qri3a.repositories.search;

import my.project.qri3a.documents.ProductDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface ProductDocRepositoryCustom {
    Page<ProductDoc> searchProductsElastic(String query,
                                           String category,
                                           String location,
                                           String condition,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           String city,
                                           Pageable pageable);
}
