package my.project.qri3a.seo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service qui génère le sitemap XML pour améliorer l'indexation par les moteurs de recherche
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SitemapGenerator {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendBaseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Génère le sitemap XML contenant les URLs des produits actifs et des profils vendeurs
     * @return Contenu du sitemap au format XML
     */
    public String generateSitemap() {
        log.info("Generating XML sitemap");
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Ajouter la page d'accueil avec priorité maximale
        appendUrl(sitemap, frontendBaseUrl, "1.0", "daily");
        
        // Ajouter les URLs des catégories principales
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=MARKET", "0.8", "weekly");
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=VEHICLES", "0.8", "weekly");
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=REAL_ESTATE", "0.8", "weekly");
        
        // Ajouter tous les produits actifs
        addProductsToSitemap(sitemap);
        
        // Ajouter les profils des vendeurs
        addSellerProfilesToSitemap(sitemap);
        
        sitemap.append("</urlset>");
        log.info("XML sitemap generated successfully");
        return sitemap.toString();
    }
    
    /**
     * Ajoute toutes les URLs des produits actifs au sitemap
     */
    private void addProductsToSitemap(StringBuilder sitemap) {
        log.debug("Adding annonces to sitemap");
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        
        for (Product product : activeProducts) {
            // Utiliser le slug au lieu de l'ID pour l'URL
            String url = product.getSlug() != null && !product.getSlug().isEmpty() 
                ? frontendBaseUrl + "/annonces/" + product.getSlug()
                : frontendBaseUrl + "/annonces/id/" + product.getId();
                
            String lastMod = formatDate(product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt());
            
            // Les produits les plus récents ont une priorité plus élevée
            String priority = calculateProductPriority(product);
            
            appendUrl(sitemap, url, priority, "daily", lastMod);
        }
        log.debug("Added {} products to sitemap", activeProducts.size());
    }
    
    /**
     * Ajoute les profils des vendeurs actifs au sitemap
     */
    private void addSellerProfilesToSitemap(StringBuilder sitemap) {
        log.debug("Adding seller profiles to sitemap");
        List<User> activeSellers = userRepository.findActiveSellersWithProducts();
        
        for (User seller : activeSellers) {
            String url = frontendBaseUrl + "/sellers/" + seller.getId();
            appendUrl(sitemap, url, "0.7", "daily");
        }
        log.debug("Added {} seller profiles to sitemap", activeSellers.size());
    }
    
    /**
     * Calcule la priorité d'un produit en fonction de sa date de création
     * Les produits plus récents ont une priorité plus élevée
     */
    private String calculateProductPriority(Product product) {
        // Cette logique peut être adaptée selon vos besoins
        LocalDateTime now = LocalDateTime.now();
        long daysSinceCreation = ChronoUnit.DAYS.between(product.getCreatedAt(), now);
        
        if (daysSinceCreation < 7) {
            return "0.9"; // Produits très récents (moins d'une semaine)
        } else if (daysSinceCreation < 30) {
            return "0.7"; // Produits récents (moins d'un mois)
        } else {
            return "0.5"; // Produits plus anciens
        }
    }
    
    /**
     * Formater une date au format YYYY-MM-DD pour le sitemap
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }
    
    /**
     * Ajouter une URL au sitemap avec les paramètres spécifiés
     */
    private void appendUrl(StringBuilder sitemap, String url, String priority, String changeFreq) {
        appendUrl(sitemap, url, priority, changeFreq, null);
    }
    
    /**
     * Ajouter une URL au sitemap avec les paramètres spécifiés, y compris la date de dernière modification
     */
    private void appendUrl(StringBuilder sitemap, String url, String priority, String changeFreq, String lastmod) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(url).append("</loc>\n");
        
        if (lastmod != null) {
            sitemap.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        
        sitemap.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
}