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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.repositories.search.ProductDocRepositoryCustom;
 
@Slf4j
@RequiredArgsConstructor
public class ProductDocRepositoryCustomImpl implements ProductDocRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductDoc> searchProductsElastic(String searchText,
                                                  String category,
                                                  String location,
                                                  String condition,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String city,
                                                  Pageable pageable) {

        // Démarrer avec un critère vide
        Criteria criteria = new Criteria();

        criteria = criteria.and(new Criteria("status").is(ProductStatus.ACTIVE.toString()));

        // Construction du critère de recherche sur le texte avec boost sur le titre
        if (searchText != null && !searchText.trim().isEmpty()) {
            String cleanedText = searchText.replace("\"", "");
            String expression = "*" + cleanedText + "*";

            // Appliquer un boost sur le champ title pour qu'il soit prioritaire sur description
            Criteria titleCriteria = new Criteria("title").expression(expression).boost(2.0f);
            Criteria descriptionCriteria = new Criteria("description").expression(expression);
            Criteria textCriteria = titleCriteria.or(descriptionCriteria);
            criteria = criteria.and(textCriteria);
        }

        // Ajout des autres filtres si fournis
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

        // Définition du tri :
        // - Si searchText est fourni, on trie d'abord sur _score (pour la pertinence de la recherche),
        //   puis sur createdAt décroissant pour départager les égalités.
        // - Sinon, on trie uniquement par createdAt décroissant.
        Sort sort;
        if (searchText != null && !searchText.trim().isEmpty()) {
            sort = Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createdAt"));
        } else {
            sort = Sort.by(Sort.Order.desc("createdAt"));
        }

        // Créer un Pageable avec le tri personnalisé
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // Création de la requête avec les critères et la pagination triée
        CriteriaQuery query = new CriteriaQuery(criteria, sortedPageable);

        // Exécution de la recherche
        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);
        List<ProductDoc> productDocs = searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(productDocs, sortedPageable, searchHits.getTotalHits());
    }


    @Override
    public List<ProductDoc> findTop10ByTitleOrDescription(String title) {
        if (title == null || title.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Clean and trim the input to remove problematic characters
        String cleanedTitle = title.replace("\"", "").trim();
        // Prepare a wildcard expression for partial matching
        String expression = "*" + cleanedTitle + "*";

        // Build criteria for either title or description using OR
        Criteria titleCriteria = new Criteria("title").expression(expression);
        Criteria descriptionCriteria = new Criteria("description").expression(expression);
        Criteria criteria = new Criteria().or(titleCriteria).or(descriptionCriteria);

        criteria = criteria.and(new Criteria("status").is(ProductStatus.ACTIVE.toString()));

        // Create the query
        CriteriaQuery query = new CriteriaQuery(criteria);

        // Limit the result to 10 items
        Pageable pageable = PageRequest.of(0, 10);
        query.setPageable(pageable);

        // Execute the query
        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);
        return searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }





}
