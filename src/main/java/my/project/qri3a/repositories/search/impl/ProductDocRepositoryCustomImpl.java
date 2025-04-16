package my.project.qri3a.repositories.search.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import my.project.qri3a.enums.ProductCategory;
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
    public List<ProductDoc> findTop10ByTitleOrDescription(String title, String category) {
        log.debug("Finding top 10 products with search term: {} and category: {}", title, category);

        if (!StringUtils.hasText(title)) {
            return Collections.emptyList();
        }

        // Construire les critères de recherche de base (similaire à searchProductsElastic)
        Criteria criteria = buildBaseCriteria(title);

        // S'assurer que nous ne récupérons que les produits actifs
        criteria = criteria.and(new Criteria(STATUS_FIELD).is(ProductStatus.ACTIVE.toString()));

        // Ajouter le filtre de catégorie s'il est spécifié (même logique que searchProductsElastic)
        if (StringUtils.hasText(category)) {
            addCategoryFilter(criteria, category);
        }

        // Créer un Pageable avec tri par pertinence (score) puis par date
        Sort sort = Sort.by(Sort.Order.desc(SCORE_FIELD), Sort.Order.desc(CREATED_AT_FIELD));
        Pageable pageable = PageRequest.of(0, MAX_SEARCH_RESULTS, sort);

        // Créer et exécuter la requête
        CriteriaQuery query = new CriteriaQuery(criteria, pageable);
        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);

        // Convertir et retourner les résultats
        return searchHits.get()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }


//    @Override
//    public List<ProductDoc> findTop10ByTitleOrDescription(String title) {
//        if (!StringUtils.hasText(title)) {
//            return Collections.emptyList();
//        }
//
//        // Construire les critères de recherche
//        Criteria criteria = buildBaseCriteria(title);
//        criteria = criteria.and(new Criteria(STATUS_FIELD).is(ProductStatus.ACTIVE.toString()));
//
//        // Créer et exécuter la requête limitée à 10 résultats
//        CriteriaQuery query = new CriteriaQuery(criteria);
//        query.setPageable(PageRequest.of(0, MAX_SEARCH_RESULTS));
//
//        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(query, ProductDoc.class);
//        return searchHits.get()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//    }

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
        addCategoryFilter(criteria, category);
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

    /**
     * Ajoute un filtre de catégorie qui prend en compte les catégories principales
     * et leurs sous-catégories associées.
     *
     * @param criteria Les critères de recherche existants
     * @param category La catégorie à filtrer
     */
    private void addCategoryFilter(Criteria criteria, String category) {
        if (!StringUtils.hasText(category)) {
            return;
        }

        String trimmedCategory = category.trim();

        // Vérifier si c'est une catégorie principale et construire une requête OR avec toutes les sous-catégories
        if (trimmedCategory.equals(ProductCategory.MARKET.toString())) {
            Criteria categoryCriteria = new Criteria(CATEGORY_FIELD).in(MARKET_SUBCATEGORIES);
            criteria = criteria.and(categoryCriteria);
        } else if (trimmedCategory.equals(ProductCategory.VEHICLES.toString())) {
            Criteria categoryCriteria = new Criteria(CATEGORY_FIELD).in(VEHICLES_SUBCATEGORIES);
            criteria = criteria.and(categoryCriteria);
        } else if (trimmedCategory.equals(ProductCategory.REAL_ESTATE.toString())) {
            Criteria categoryCriteria = new Criteria(CATEGORY_FIELD).in(REAL_ESTATE_SUBCATEGORIES);
            criteria = criteria.and(categoryCriteria);
        } else {
            // Si ce n'est pas une catégorie principale, appliquer le filtre normal
            criteria = criteria.and(new Criteria(CATEGORY_FIELD).is(trimmedCategory));
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

    /**
     * Nettoie et prépare le texte de recherche pour Elasticsearch en :
     * 1. Échappant les caractères spéciaux
     * 2. Normalisant les espaces multiples
     * 3. Limitant la longueur maximale de la requête
     * 4. Convertissant en minuscules pour une recherche insensible à la casse
     * 5. Traitant les termes vides ou null
     *
     * @param text Le texte de recherche brut
     * @return Le texte nettoyé et préparé pour Elasticsearch
     */
    private String cleanSearchText(String text) {
        // Vérification des entrées nulles ou vides
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Limiter la longueur maximale de la recherche (évite les attaques par injection de requêtes très longues)
        final int MAX_QUERY_LENGTH = 100;
        if (text.length() > MAX_QUERY_LENGTH) {
            text = text.substring(0, MAX_QUERY_LENGTH);
        }

        // Convertir en minuscules pour une recherche insensible à la casse
        text = text.toLowerCase();

        // Échapper les caractères spéciaux d'Elasticsearch
        // + - = && || > < ! ( ) { } [ ] ^ " ~ * ? : \ / et autres caractères pouvant causer des problèmes
        String escaped = text.replaceAll("([+\\-=&|><!(){}\\[\\]^\"~*?:/\\\\])", "\\\\$1");

        // Supprimer les caractères de contrôle qui peuvent causer des problèmes dans ES
        escaped = escaped.replaceAll("[\\p{Cntrl}]", "");

        // Normaliser les espaces multiples en un seul espace
        escaped = escaped.replaceAll("\\s+", " ");

        // Enlever les espaces de début et de fin
        return escaped.trim();
    }


}