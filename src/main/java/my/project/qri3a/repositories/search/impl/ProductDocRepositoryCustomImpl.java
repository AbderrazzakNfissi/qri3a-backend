package my.project.qri3a.repositories.search.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.repositories.search.ProductDocRepositoryCustom;

@Slf4j
@Repository
public class ProductDocRepositoryCustomImpl implements ProductDocRepositoryCustom {

    private static final String INDEX_NAME = "products_idx";
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
    private static final int MAX_SEARCH_RESULTS = 10;

    // Définir les mappings des catégories principales vers leurs sous-catégories
    private static final Set<String> MARKET_SUBCATEGORIES = new HashSet<>(Arrays.asList(
            ProductCategory.SMARTPHONES_AND_TELEPHONES.toString(),
            ProductCategory.TABLETS_AND_E_BOOKS.toString(),
            ProductCategory.LAPTOPS.toString(),
            ProductCategory.DESKTOP_COMPUTERS.toString(),
            ProductCategory.TELEVISIONS.toString(),
            ProductCategory.ELECTRO_MENAGE.toString(),
            ProductCategory.ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS.toString(),
            ProductCategory.SMARTWATCHES_AND_ACCESSORIES.toString(),
            ProductCategory.AUDIO_AND_HIFI.toString(),
            ProductCategory.COMPUTER_COMPONENTS.toString(),
            ProductCategory.STORAGE_AND_PERIPHERALS.toString(),
            ProductCategory.PRINTERS_AND_SCANNERS.toString(),
            ProductCategory.DRONES_AND_ACCESSORIES.toString(),
            ProductCategory.NETWORK_EQUIPMENT.toString(),
            ProductCategory.SMART_HOME_DEVICES.toString(),
            ProductCategory.GAMING_ACCESSORIES.toString(),
            ProductCategory.PHOTO_AND_VIDEO_EQUIPMENT.toString()
    ));

    private static final Set<String> VEHICLES_SUBCATEGORIES = new HashSet<>(Arrays.asList(
            ProductCategory.CARS.toString(),
            ProductCategory.MOTORCYCLES.toString(),
            ProductCategory.BICYCLES.toString(),
            ProductCategory.VEHICLE_PARTS.toString(),
            ProductCategory.TRUCKS_AND_MACHINERY.toString(),
            ProductCategory.BOATS.toString(),
            ProductCategory.OTHER_VEHICLES.toString()
    ));

    private static final Set<String> REAL_ESTATE_SUBCATEGORIES = new HashSet<>(Arrays.asList(
            ProductCategory.REAL_ESTATE_SALES.toString(),
            ProductCategory.APARTMENTS_FOR_SALE.toString(),
            ProductCategory.HOUSES_FOR_SALE.toString(),
            ProductCategory.VILLAS_RIADS_FOR_SALE.toString(),
            ProductCategory.OFFICES_FOR_SALE.toString(),
            ProductCategory.COMMERCIAL_SPACES_FOR_SALE.toString(),
            ProductCategory.LAND_AND_FARMS_FOR_SALE.toString(),
            ProductCategory.OTHER_REAL_ESTATE_FOR_SALE.toString(),
            ProductCategory.REAL_ESTATE_RENTALS.toString(),
            ProductCategory.APARTMENTS_FOR_RENT.toString(),
            ProductCategory.HOUSES_FOR_RENT.toString(),
            ProductCategory.VILLAS_RIADS_FOR_RENT.toString(),
            ProductCategory.OFFICES_FOR_RENT.toString(),
            ProductCategory.COMMERCIAL_SPACES_FOR_RENT.toString(),
            ProductCategory.LAND_AND_FARMS_FOR_RENT.toString(),
            ProductCategory.OTHER_REAL_ESTATE_FOR_RENT.toString()
    ));

    private final RestHighLevelClient openSearchClient;
    private final ObjectMapper objectMapper;

    // Constructeur avec dépendances standard
    public ProductDocRepositoryCustomImpl(RestHighLevelClient openSearchClient, ObjectMapper objectMapper) {
        this.openSearchClient = openSearchClient;
        this.objectMapper = objectMapper;
    }

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

        try {
            // Construire la requête de recherche
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // Construire la requête de base
            BoolQueryBuilder boolQuery = buildBaseQuery(searchText);

            // Ajouter les filtres supplémentaires
            addFilterQueries(boolQuery, category, location, condition, minPrice, maxPrice, city, delivery);

            // Configurer la pagination
            int from = pageable.getPageNumber() * pageable.getPageSize();
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(pageable.getPageSize());

            // Configurer le tri
            configureSorting(searchSourceBuilder, searchText, pageable);

            // Finaliser la requête
            searchSourceBuilder.query(boolQuery);
            searchRequest.source(searchSourceBuilder);

            // Exécuter la recherche
            SearchResponse searchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Transformer les résultats
            return processSearchResponse(searchResponse, pageable);

        } catch (IOException e) {
            log.error("Error executing OpenSearch query", e);
            return Page.empty();
        }
    }

    @Override
    public List<ProductDoc> findTop10ByTitleOrDescription(String title, String category) {
        log.debug("Finding top 10 products with search term: {} and category: {}", title, category);

        if (!StringUtils.hasText(title)) {
            return Collections.emptyList();
        }

        try {
            // Construire la requête de recherche
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // Construire la requête de base mais en utilisant des requêtes plus souples
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Filtre pour produits actifs
            boolQuery.must(QueryBuilders.termQuery(STATUS_FIELD, ProductStatus.ACTIVE.toString()));

            // Nettoyer le texte
            String cleanedText = cleanSearchText(title);

            // Utiliser un multi_match pour une meilleure correspondance textuelle
            boolQuery.must(QueryBuilders.multiMatchQuery(cleanedText, TITLE_FIELD, DESCRIPTION_FIELD)
                    .type("phrase_prefix")
                    .slop(3));

            // Ajouter le filtre de catégorie si spécifié
            if (StringUtils.hasText(category)) {
                addCategoryFilter(boolQuery, category);
            }

            // Limiter à 10 résultats
            searchSourceBuilder.size(MAX_SEARCH_RESULTS);

            // Configurer le tri par score puis par date
            searchSourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
            searchSourceBuilder.sort(SortBuilders.fieldSort(CREATED_AT_FIELD).order(SortOrder.DESC));

            // Finaliser la requête
            searchSourceBuilder.query(boolQuery);
            searchRequest.source(searchSourceBuilder);

            // Exécuter la recherche
            SearchResponse searchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Transformer les résultats
            List<ProductDoc> products = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                try {
                    ProductDoc product = objectMapper.readValue(hit.getSourceAsString(), ProductDoc.class);
                    products.add(product);
                } catch (IOException e) {
                    log.error("Error converting SearchHit to ProductDoc", e);
                }
            }
            return products;

        } catch (IOException e) {
            log.error("Error executing OpenSearch query for top 10 products", e);
            return Collections.emptyList();
        }
    }

    // Méthodes privées d'assistance

    private BoolQueryBuilder buildBaseQuery(String searchText) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Filtre pour produits actifs
        boolQuery.must(QueryBuilders.termQuery(STATUS_FIELD, ProductStatus.ACTIVE.toString()));

        if (StringUtils.hasText(searchText)) {
            // Nettoyer et préparer le texte de recherche
            String cleanedText = cleanSearchText(searchText);

            // Diviser la requête en termes individuels pour une recherche plus flexible
            String[] searchTerms = cleanedText.split("\\s+");

            BoolQueryBuilder textQuery = QueryBuilders.boolQuery();

            for (String term : searchTerms) {
                // Recherche avec match_phrase_prefix pour meilleure correspondance textuelle
                textQuery.should(QueryBuilders.matchPhrasePrefixQuery(TITLE_FIELD, term));
                textQuery.should(QueryBuilders.matchPhrasePrefixQuery(DESCRIPTION_FIELD, term));

                // Recherche wildcard comme fallback
                textQuery.should(QueryBuilders.wildcardQuery(TITLE_FIELD, "*" + term + "*"));
                textQuery.should(QueryBuilders.wildcardQuery(DESCRIPTION_FIELD, "*" + term + "*"));
            }

            // Utiliser un multi_match pour la requête complète également
            textQuery.should(QueryBuilders.multiMatchQuery(cleanedText, TITLE_FIELD, DESCRIPTION_FIELD)
                    .type("phrase_prefix")
                    .slop(3));

            boolQuery.must(textQuery);
        }

        return boolQuery;
    }

    private void addFilterQueries(BoolQueryBuilder boolQuery,
                                  String category,
                                  String location,
                                  String condition,
                                  BigDecimal minPrice,
                                  BigDecimal maxPrice,
                                  String city,
                                  String delivery) {
        // Ajouter les filtres si fournis
        addCategoryFilter(boolQuery, category);
        addStringFilter(boolQuery, LOCATION_FIELD, location);
        addStringFilter(boolQuery, CONDITION_FIELD, condition);
        addStringFilter(boolQuery, CITY_FIELD, city);
        addStringFilter(boolQuery, DELIVERY_FIELD, delivery);

        // Ajouter les filtres de prix
        if (minPrice != null || maxPrice != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(PRICE_FIELD);

            if (minPrice != null) {
                rangeQuery.gte(minPrice.doubleValue());
            }

            if (maxPrice != null) {
                rangeQuery.lte(maxPrice.doubleValue());
            }

            boolQuery.must(rangeQuery);
        }
    }

    private void addCategoryFilter(BoolQueryBuilder boolQuery, String category) {
        if (!StringUtils.hasText(category)) {
            return;
        }

        String trimmedCategory = category.trim();

        if (trimmedCategory.equals(ProductCategory.MARKET.toString())) {
            BoolQueryBuilder categoryQuery = QueryBuilders.boolQuery();
            for (String subCategory : MARKET_SUBCATEGORIES) {
                categoryQuery.should(QueryBuilders.termQuery(CATEGORY_FIELD, subCategory));
            }
            boolQuery.must(categoryQuery);
        } else if (trimmedCategory.equals(ProductCategory.VEHICLES.toString())) {
            BoolQueryBuilder categoryQuery = QueryBuilders.boolQuery();
            for (String subCategory : VEHICLES_SUBCATEGORIES) {
                categoryQuery.should(QueryBuilders.termQuery(CATEGORY_FIELD, subCategory));
            }
            boolQuery.must(categoryQuery);
        } else if (trimmedCategory.equals(ProductCategory.REAL_ESTATE.toString())) {
            BoolQueryBuilder categoryQuery = QueryBuilders.boolQuery();
            for (String subCategory : REAL_ESTATE_SUBCATEGORIES) {
                categoryQuery.should(QueryBuilders.termQuery(CATEGORY_FIELD, subCategory));
            }
            boolQuery.must(categoryQuery);
        } else {
            boolQuery.must(QueryBuilders.termQuery(CATEGORY_FIELD, trimmedCategory));
        }
    }

    private void addStringFilter(BoolQueryBuilder boolQuery, String field, String value) {
        if (StringUtils.hasText(value)) {
            boolQuery.must(QueryBuilders.termQuery(field, value.trim()));
        }
    }

    private void configureSorting(SearchSourceBuilder searchSourceBuilder, String searchText, Pageable pageable) {
        if (StringUtils.hasText(searchText)) {
            // Si recherche par texte, trier d'abord par score puis par date
            searchSourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
            searchSourceBuilder.sort(SortBuilders.fieldSort(CREATED_AT_FIELD).order(SortOrder.DESC));
        } else {
            // Sinon, trier par date uniquement
            searchSourceBuilder.sort(SortBuilders.fieldSort(CREATED_AT_FIELD).order(SortOrder.DESC));
        }

        // Ajouter les tris personnalisés si définis dans le Pageable
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                SortOrder sortOrder = order.isAscending() ? SortOrder.ASC : SortOrder.DESC;
                searchSourceBuilder.sort(SortBuilders.fieldSort(order.getProperty()).order(sortOrder));
            });
        }
    }

    private Page<ProductDoc> processSearchResponse(SearchResponse searchResponse, Pageable pageable) {
        List<ProductDoc> productDocs = new ArrayList<>();

        for (SearchHit hit : searchResponse.getHits().getHits()) {
            try {
                ProductDoc productDoc = objectMapper.readValue(hit.getSourceAsString(), ProductDoc.class);
                productDocs.add(productDoc);
            } catch (IOException e) {
                log.error("Error converting SearchHit to ProductDoc", e);
            }
        }

        long totalHits = searchResponse.getHits().getTotalHits().value;
        return new PageImpl<>(productDocs, pageable, totalHits);
    }

    private String cleanSearchText(String text) {
        // Vérification des entrées nulles ou vides
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Limiter la longueur maximale de la recherche
        final int MAX_QUERY_LENGTH = 100;
        if (text.length() > MAX_QUERY_LENGTH) {
            text = text.substring(0, MAX_QUERY_LENGTH);
        }

        // Convertir en minuscules
        text = text.toLowerCase();

        // Supprimer uniquement les caractères dangereux pour Elasticsearch
        // Ne PAS échapper les chiffres ou autres caractères normaux
        String cleaned = text.replaceAll("[\\p{Cntrl}]", "");

        // Normaliser les espaces multiples
        cleaned = cleaned.replaceAll("\\s+", " ");

        // Enlever les espaces de début et de fin
        return cleaned.trim();
    }
}