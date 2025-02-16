package my.project.qri3a.repositories.search.impl;

import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.repositories.search.ProductDocRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ProductDocRepositoryCustomImpl implements ProductDocRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    public ProductDocRepositoryCustomImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public Page<ProductDoc> searchProductsElastic(String searchText,
                                                  String category,
                                                  String location,
                                                  String condition,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String city,
                                                  Pageable pageable) {

        // Start with an empty Criteria
        Criteria criteria = new Criteria();

        // Build text search criteria:
        if (searchText != null && !searchText.trim().isEmpty()) {
            // Remove any double quotes to avoid issues in the wildcard query.
            String cleanedText = searchText.replace("\"", "");
            // Add wildcards manually if you want partial matching.
            String expression = "*" + cleanedText + "*";

            // Use the expression() method instead of contains().
            Criteria textCriteria = new Criteria("title").expression(expression)
                    .or(new Criteria("description").expression(expression));
            criteria = criteria.and(textCriteria);
        }

        // Add other filters only if provided:
        if (category != null && !category.trim().isEmpty()) {
            criteria = criteria.and(new Criteria("category.keyword").is(category));
        }
        if (location != null && !location.trim().isEmpty()) {
            criteria = criteria.and(new Criteria("location.keyword").is(location));
        }
        if (condition != null && !condition.trim().isEmpty()) {
            criteria = criteria.and(new Criteria("condition.keyword").is(condition));
        }
        if (city != null && !city.trim().isEmpty()) {
            criteria = criteria.and(new Criteria("city.keyword").is(city));
        }
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }

        // Build the query with pagination
        CriteriaQuery query = new CriteriaQuery(criteria, pageable);

        // Execute the search
        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);
        List<ProductDoc> productDocs = searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(productDocs, pageable, searchHits.getTotalHits());
    }
}
