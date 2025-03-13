package my.project.qri3a.repositories.search.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import my.project.qri3a.enums.ProductStatus;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.repositories.search.ProductDocRepositoryCustom;

@Slf4j
@RequiredArgsConstructor
public class ProductDocRepositoryCustomImpl implements ProductDocRepositoryCustom {

    private static final String STATUS_FIELD = "status";
    private static final String TITLE_FIELD = "title";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String CATEGORY_FIELD = "category.keyword";
    private static final String LOCATION_FIELD = "location.keyword";
    private static final String CONDITION_FIELD = "condition.keyword";
    private static final String CITY_FIELD = "city.keyword";
    private static final String PRICE_FIELD = "price";
    private static final String DELIVERY_FIELD = "delivery.keyword";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String SCORE_FIELD = "_score";
    private static final int MAX_SEARCH_RESULTS = 10;

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductDoc> searchProductsElastic(String searchText,
                                                  String category,
                                                  String location,
                                                  String condition,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String city,
                                                  String delivery,
                                                  Pageable pageable) {
        log.debug("Searching products with criteria: {}", searchText);

        // Construire les critères de recherche
        Criteria criteria = buildBaseCriteria(searchText);

        // Ajouter les filtres supplémentaires
        addFilterCriteria(criteria, category, location, condition, minPrice, maxPrice, city, delivery);

        // Créer un Pageable avec le tri personnalisé
        Pageable sortedPageable = createSortedPageable(searchText, pageable);

        // Exécuter la recherche
        return executeSearch(criteria, sortedPageable);
    }

    @Override
    public List<ProductDoc> findTop10ByTitleOrDescription(String title) {
        if (!StringUtils.hasText(title)) {
            return Collections.emptyList();
        }

        // Construire les critères de recherche
        Criteria criteria = buildBaseCriteria(title);
        criteria = criteria.and(new Criteria(STATUS_FIELD).is(ProductStatus.ACTIVE.toString()));

        // Créer et exécuter la requête limitée à 10 résultats
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, MAX_SEARCH_RESULTS));

        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);
        return searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    // Méthodes privées d'assistance

    private Criteria buildBaseCriteria(String searchText) {
        Criteria baseCriteria = new Criteria();
        baseCriteria = baseCriteria.and(new Criteria(STATUS_FIELD).is("ACTIVE"));

        if (StringUtils.hasText(searchText)) {
            // Nettoyer et préparer le texte de recherche
            String cleanedText = cleanSearchText(searchText);
            String expression = "*" + cleanedText + "*";

            // Construire les critères pour le titre ou la description
            Criteria titleCriteria = new Criteria(TITLE_FIELD).expression(expression);
            Criteria descriptionCriteria = new Criteria(DESCRIPTION_FIELD).expression(expression);
            baseCriteria = new Criteria().or(titleCriteria).or(descriptionCriteria);
        }

        return baseCriteria;
    }

    private void addFilterCriteria(Criteria criteria,
                                   String category,
                                   String location,
                                   String condition,
                                   BigDecimal minPrice,
                                   BigDecimal maxPrice,
                                   String city,
                                   String delivery) {
        // Ajouter les filtres si fournis
        addStringFilter(criteria, CATEGORY_FIELD, category);
        addStringFilter(criteria, LOCATION_FIELD, location);
        addStringFilter(criteria, CONDITION_FIELD, condition);
        addStringFilter(criteria, CITY_FIELD, city);
        addStringFilter(criteria, DELIVERY_FIELD, delivery);

        // Ajouter les filtres de prix
        if (minPrice != null) {
            criteria = criteria.and(new Criteria(PRICE_FIELD).greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria(PRICE_FIELD).lessThanEqual(maxPrice));
        }
    }

    private void addStringFilter(Criteria criteria, String field, String value) {
        if (StringUtils.hasText(value)) {
            criteria = criteria.and(new Criteria(field).is(value.trim()));
        }
    }

    private Pageable createSortedPageable(String searchText, Pageable pageable) {
        Sort sort;
        if (StringUtils.hasText(searchText)) {
            sort = Sort.by(Sort.Order.desc(SCORE_FIELD), Sort.Order.desc(CREATED_AT_FIELD));
        } else {
            sort = Sort.by(Sort.Order.desc(CREATED_AT_FIELD));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Page<ProductDoc> executeSearch(Criteria criteria, Pageable pageable) {
        CriteriaQuery query = new CriteriaQuery(criteria, pageable);
        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);

        List<ProductDoc> productDocs = searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(productDocs, pageable, searchHits.getTotalHits());
    }

    private String cleanSearchText(String text) {
        return text.replace("\"", "").trim();
    }
}