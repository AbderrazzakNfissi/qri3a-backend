package my.project.qri3a.repositories.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.documents.ProductDoc;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductDocRepository implements ProductDocRepositoryCustom {

    private static final String INDEX_NAME = "products_idx";

    private final RestHighLevelClient openSearchClient;
    private final ObjectMapper objectMapper;
    private final ProductDocRepositoryCustom productDocRepositoryCustom;

    /**
     * Enregistre un document produit dans OpenSearch
     *
     * @param productDoc Le document produit à enregistrer
     * @return Le document produit enregistré
     */
    public ProductDoc save(ProductDoc productDoc) {
        try {
            String productJson = objectMapper.writeValueAsString(productDoc);

            IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                    .id(productDoc.getId().toString())
                    .source(productJson, XContentType.JSON);

            openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
            return productDoc;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement du produit dans OpenSearch", e);
            throw new RuntimeException("Erreur lors de l'enregistrement du produit", e);
        }
    }

    /**
     * Enregistre une liste de documents produits dans OpenSearch
     *
     * @param productDocs La liste des documents produits à enregistrer
     * @return La liste des documents produits enregistrés
     */
    public Iterable<ProductDoc> saveAll(Iterable<ProductDoc> productDocs) {
        try {
            BulkRequest bulkRequest = new BulkRequest();

            for (ProductDoc productDoc : productDocs) {
                String productJson = objectMapper.writeValueAsString(productDoc);

                IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                        .id(productDoc.getId().toString())
                        .source(productJson, XContentType.JSON);

                bulkRequest.add(indexRequest);
            }

            BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                log.error("Erreur lors de l'enregistrement en masse des produits: {}", bulkResponse.buildFailureMessage());
            }

            return productDocs;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement en masse des produits dans OpenSearch", e);
            throw new RuntimeException("Erreur lors de l'enregistrement en masse des produits", e);
        }
    }

    /**
     * Supprime un document produit par ID
     *
     * @param id L'ID du document produit à supprimer
     */
    public void deleteById(UUID id) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, id.toString());
            openSearchClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du produit dans OpenSearch", e);
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }

    /**
     * Supprime une liste de documents produits par leurs IDs
     *
     * @param ids La liste des IDs des documents produits à supprimer
     */
    @Transactional
    public void deleteByIdIn(List<UUID> ids) {
        try {
            BulkRequest bulkRequest = new BulkRequest();

            for (UUID id : ids) {
                DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, id.toString());
                bulkRequest.add(deleteRequest);
            }

            BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                log.error("Erreur lors de la suppression en masse des produits: {}", bulkResponse.buildFailureMessage());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la suppression en masse des produits dans OpenSearch", e);
            throw new RuntimeException("Erreur lors de la suppression en masse des produits", e);
        }
    }

    /**
     * Trouve tous les documents produits avec le statut spécifié, triés par date de création
     *
     * @param status Le statut des produits à rechercher
     * @param pageable La pagination
     * @return Une page de documents produits
     */
    public Page<ProductDoc> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable) {
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // Construire la requête pour le statut
            searchSourceBuilder.query(QueryBuilders.termQuery("status", status));

            // Configurer la pagination
            int from = pageable.getPageNumber() * pageable.getPageSize();
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(pageable.getPageSize());

            // Configurer le tri par date de création
            searchSourceBuilder.sort(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));

            searchRequest.source(searchSourceBuilder);

            // Exécuter la recherche
            SearchResponse searchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Transformer les résultats
            return processSearchResponse(searchResponse, pageable);
        } catch (IOException e) {
            log.error("Erreur lors de la recherche des produits par statut dans OpenSearch", e);
            return Page.empty();
        }
    }

    /**
     * Trouve tous les documents produits dont le titre ou la description contient le terme spécifié
     *
     * @param title Le terme à rechercher dans le titre
     * @param description Le terme à rechercher dans la description
     * @param pageable La pagination
     * @return Une page de documents produits
     */
    public Page<ProductDoc> findProductDocByTitleOrDescription(String title, String description, Pageable pageable) {
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // Construire la requête pour le titre ou la description
            searchSourceBuilder.query(
                    QueryBuilders.boolQuery()
                            .should(QueryBuilders.wildcardQuery("title", "*" + title + "*"))
                            .should(QueryBuilders.wildcardQuery("description", "*" + description + "*"))
            );

            // Configurer la pagination
            int from = pageable.getPageNumber() * pageable.getPageSize();
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(pageable.getPageSize());

            searchRequest.source(searchSourceBuilder);

            // Exécuter la recherche
            SearchResponse searchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Transformer les résultats
            return processSearchResponse(searchResponse, pageable);
        } catch (IOException e) {
            log.error("Erreur lors de la recherche des produits par titre ou description dans OpenSearch", e);
            return Page.empty();
        }
    }

    /**
     * Implémentation de l'interface ProductDocRepositoryCustom
     */
    @Override
    public Page<ProductDoc> searchProductsElastic(String query, String category, String location, String condition,
                                                  java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String city, String delivery, Pageable pageable) {
        return productDocRepositoryCustom.searchProductsElastic(query, category, location, condition, minPrice, maxPrice, city, delivery, pageable);
    }

    @Override
    public List<ProductDoc> findTop10ByTitleOrDescription(String title, String category) {
        return productDocRepositoryCustom.findTop10ByTitleOrDescription(title, category);
    }

    /**
     * Transforme une réponse de recherche OpenSearch en une page de documents produits
     */
    private Page<ProductDoc> processSearchResponse(SearchResponse searchResponse, Pageable pageable) {
        List<ProductDoc> productDocs = new ArrayList<>();

        for (SearchHit hit : searchResponse.getHits().getHits()) {
            try {
                ProductDoc productDoc = objectMapper.readValue(hit.getSourceAsString(), ProductDoc.class);
                productDocs.add(productDoc);
            } catch (IOException e) {
                log.error("Erreur lors de la conversion du résultat de recherche en ProductDoc", e);
            }
        }

        long totalHits = searchResponse.getHits().getTotalHits().value;
        return new PageImpl<>(productDocs, pageable, totalHits);
    }
}