package my.project.qri3a.seo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.project.qri3a.dtos.responses.ProductResponseDTO;
import my.project.qri3a.enums.ProductCondition;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Générateur de contenu JSON-LD pour améliorer le SEO
 * Implémente le schéma schema.org pour les produits et autres entités
 */
@Component
public class JsonLdGenerator {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public JsonLdGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Génère le JSON-LD pour un produit
     * @param product Le DTO du produit
     * @return JSON-LD au format String
     */
    public String generateProductJsonLd(ProductResponseDTO product) {
        Map<String, Object> jsonLd = new HashMap<>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "Product");
        jsonLd.put("name", product.getTitle());
        jsonLd.put("description", product.getDescription());

        // Ajouter les images du produit
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            jsonLd.put("image", product.getImages().get(0).getUrl());
        }

        // Information sur le prix
        Map<String, Object> offer = new HashMap<>();
        offer.put("@type", "Offer");
        offer.put("price", product.getPrice());
        offer.put("priceCurrency", "MAD"); // Dirham marocain
        offer.put("availability", "https://schema.org/InStock");
        offer.put("itemCondition", mapConditionToSchemaOrgFormat(product.getCondition()));
        jsonLd.put("offers", offer);

        // Information sur le vendeur - nous utilisons le champ user à la place de seller
        if (product.getUser() != null) {
            Map<String, Object> seller = new HashMap<>();
            seller.put("@type", "Person");
            seller.put("name", product.getUser().getName());
            jsonLd.put("seller", seller);
        }

        // Localisation du produit
        Map<String, Object> location = new HashMap<>();
        location.put("@type", "Place");
        location.put("name", product.getLocation() + ", " + product.getCity());
        jsonLd.put("locationCreated", location);

        // Catégorie du produit
        jsonLd.put("category", product.getCategory().toString());
        
        // Date de création
        jsonLd.put("dateCreated", product.getCreatedAt());
        
        // Date de modification - si non disponible, on utilise la date de création
        jsonLd.put("dateModified", product.getCreatedAt()); // Remplacement de updatedAt par createdAt
        
        try {
            return objectMapper.writeValueAsString(jsonLd);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    /**
     * Convertit l'état du produit au format schema.org
     */
    /**
     * Convertit l'état du produit au format schema.org
     */
    private String mapConditionToSchemaOrgFormat(ProductCondition condition) {
        if (condition == null) {
            return "https://schema.org/UsedCondition";
        }

        switch (condition) {
            case NEW:
                return "https://schema.org/NewCondition";
            case USED:
                // Decide what to return for BOTH - perhaps default to UsedCondition
                return "https://schema.org/UsedCondition";
            case BOTH:
            default:
                return "https://schema.org/UsedCondition";
        }
    }
}