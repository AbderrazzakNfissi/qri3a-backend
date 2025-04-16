package my.project.qri3a.repositories.search;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import my.project.qri3a.documents.ProductDoc;

public interface ProductDocRepositoryCustom {
    Page<ProductDoc> searchProductsElastic(String query,
                                           String category,
                                           String location,
                                           String condition,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           String city,
                                           String delivery,
                                           Pageable pageable);

    List<ProductDoc> findTop10ByTitleOrDescription(String title, String category);

}
