package my.project.qri3a.repositories.search;

import my.project.qri3a.documents.ProductDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductDocRepository extends ElasticsearchRepository<ProductDoc, UUID> , ProductDocRepositoryCustom{

    Page<ProductDoc> findAllByOrderByCreatedAtAsc(Pageable pageable);
    /*
    @Query("{\n" +
            "  \"bool\": {\n" +
            "    \"must\": [\n" +
            "      {\n" +
            "        \"multi_match\": {\n" +
            "          \"query\": \"?0\",\n" +
            "          \"fields\": [\"title\", \"description\"]\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"filter\": [\n" +
            "      { \"term\": { \"category.keyword\": \"?1\" }},\n" +
            "      { \"term\": { \"location.keyword\": \"?2\" }},\n" +
            "      { \"term\": { \"condition.keyword\": \"?3\" }},\n" +
            "      { \"range\": { \"price\": { \"gte\": ?4, \"lte\": ?5 } }},\n" +
            "      { \"term\": { \"city.keyword\": \"?6\" }}\n" +
            "    ]\n" +
            "  }\n" +
            "}")
    Page<ProductDoc> searchProductsElastic(String query,
                                           String category,
                                           String location,
                                           String condition,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           String city,
                                           Pageable pageable);
  */
    Page<ProductDoc> findProductDocByTitleOrDescription(String title,String description, Pageable pageable);

}
