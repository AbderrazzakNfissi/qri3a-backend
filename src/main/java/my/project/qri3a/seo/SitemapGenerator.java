package my.project.qri3a.seo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Value("${app.backend.url:http://localhost:8081}")
    private String backendBaseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    // Nombre maximum d'URLs par fichier sitemap (Google recommande 50000 max)
    private static final int MAX_URLS_PER_SITEMAP = 45000;
    
    /**
     * Génère le sitemap index XML qui référence tous les autres sitemaps
     * @return Contenu du sitemap index au format XML
     */
    public String generateSitemapIndex() {
        log.info("Generating XML sitemap index");
        StringBuilder sitemapIndex = new StringBuilder();
        sitemapIndex.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemapIndex.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        LocalDateTime now = LocalDateTime.now();
        String lastmod = now.atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMATTER);
        
        // Sitemap statique (pages principales)
        sitemapIndex.append("  <sitemap>\n");
        sitemapIndex.append("    <loc>").append(backendBaseUrl).append("/api/v1/seo/sitemap-static.xml</loc>\n");
        sitemapIndex.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        sitemapIndex.append("  </sitemap>\n");
        
        // Nombre de pages de sitemap nécessaires
        long totalActiveProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        int numSitemapFiles = (int) Math.ceil((double) totalActiveProducts / MAX_URLS_PER_SITEMAP);
        
        // Sitemaps pour les produits (listings)
        for (int i = 1; i <= numSitemapFiles; i++) {
            sitemapIndex.append("  <sitemap>\n");
            sitemapIndex.append("    <loc>").append(backendBaseUrl).append("/api/v1/seo/sitemap-listings-").append(i).append(".xml</loc>\n");
            sitemapIndex.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
            sitemapIndex.append("  </sitemap>\n");
        }
        
        // Sitemaps par catégorie principale
        for (String mainCategory : List.of("MARKET", "VEHICLES", "REAL_ESTATE")) {
            sitemapIndex.append("  <sitemap>\n");
            sitemapIndex.append("    <loc>").append(backendBaseUrl).append("/api/v1/seo/sitemap-category-").append(mainCategory.toLowerCase()).append(".xml</loc>\n");
            sitemapIndex.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
            sitemapIndex.append("  </sitemap>\n");
        }
        
        // Sitemap pour les profils vendeurs
        sitemapIndex.append("  <sitemap>\n");
        sitemapIndex.append("    <loc>").append(backendBaseUrl).append("/api/v1/seo/sitemap-sellers.xml</loc>\n");
        sitemapIndex.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        sitemapIndex.append("  </sitemap>\n");
        
        sitemapIndex.append("</sitemapindex>");
        log.info("XML sitemap index generated successfully");
        return sitemapIndex.toString();
    }

    /**
     * Génère le sitemap statique contenant les URLs des pages principales
     * @return Contenu du sitemap au format XML
     */
    public String generateStaticSitemap() {
        log.info("Generating static XML sitemap");
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Ajouter la page d'accueil avec priorité maximale
        appendUrl(sitemap, frontendBaseUrl, "1.0", "daily");
        
        // Ajouter les pages principales
        appendUrl(sitemap, frontendBaseUrl + "/about-us", "0.8", "monthly");
        appendUrl(sitemap, frontendBaseUrl + "/contact-us", "0.7", "monthly");
        appendUrl(sitemap, frontendBaseUrl + "/conditions-utilisation", "0.6", "monthly");
        appendUrl(sitemap, frontendBaseUrl + "/security", "0.6", "monthly");
        appendUrl(sitemap, frontendBaseUrl + "/report-scam", "0.6", "monthly");
        
        // Ajouter les URLs des catégories principales
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=MARKET", "0.9", "daily");
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=VEHICLES", "0.9", "daily");
        appendUrl(sitemap, frontendBaseUrl + "/annonces/search?category=REAL_ESTATE", "0.9", "daily");
        
        // Ajouter les sous-catégories populaires
        Map<String, List<String>> subcategories = getPopularSubcategories();
        for (Map.Entry<String, List<String>> entry : subcategories.entrySet()) {
            String mainCategory = entry.getKey();
            for (String subCategory : entry.getValue()) {
                String url = frontendBaseUrl + "/annonces/search?category=" + subCategory;
                appendUrl(sitemap, url, "0.8", "weekly");
            }
        }
        
        sitemap.append("</urlset>");
        log.info("Static XML sitemap generated successfully");
        return sitemap.toString();
    }
    
    /**
     * Génère un sitemap pour les produits avec pagination
     * @param pageNum Le numéro de la page du sitemap
     * @return Contenu du sitemap au format XML
     */
    public String generateProductsSitemap(int pageNum) {
        log.info("Generating products XML sitemap page {}", pageNum);
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        int pageSize = MAX_URLS_PER_SITEMAP;
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Product> productsPage = productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE, pageable);
        
        for (Product product : productsPage.getContent()) {
            // Utiliser le slug au lieu de l'ID pour l'URL
            String url = product.getSlug() != null && !product.getSlug().isEmpty() 
                ? frontendBaseUrl + "/annonces/" + product.getSlug()
                : frontendBaseUrl + "/annonces/id/" + product.getId();
                
            String lastMod = formatDate(product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt());
            
            // Les produits les plus récents ont une priorité plus élevée
            String priority = calculateProductPriority(product);
            
            appendUrl(sitemap, url, priority, "weekly", lastMod);
        }
        
        sitemap.append("</urlset>");
        log.info("Products XML sitemap page {} generated successfully with {} products", pageNum, productsPage.getNumberOfElements());
        return sitemap.toString();
    }
    
    /**
     * Génère un sitemap pour une catégorie principale spécifique
     * @param mainCategory La catégorie principale
     * @return Contenu du sitemap au format XML
     */
    public String generateCategorySitemap(String mainCategory) {
        log.info("Generating category XML sitemap for {}", mainCategory);
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        try {
            ProductCategory category = ProductCategory.valueOf(mainCategory.toUpperCase());
            List<String> subcategories = getSubcategoriesForMainCategory(category)
                .stream()
                .map(ProductCategory::name)
                .collect(Collectors.toList());
            
            for (String subcategory : subcategories) {
                // Obtenir quelques produits de cette sous-catégorie
                Pageable pageable = PageRequest.of(0, 1000); // Limité à 1000 par sous-catégorie
                Page<Product> products = productRepository.findByStatusAndCategoryOrderByCreatedAtDesc(
                    ProductStatus.ACTIVE, 
                    ProductCategory.valueOf(subcategory), 
                    pageable
                );
                
                for (Product product : products.getContent()) {
                    String url = product.getSlug() != null && !product.getSlug().isEmpty() 
                        ? frontendBaseUrl + "/annonces/" + product.getSlug()
                        : frontendBaseUrl + "/annonces/id/" + product.getId();
                        
                    String lastMod = formatDate(product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt());
                    String priority = calculateProductPriority(product);
                    
                    appendUrl(sitemap, url, priority, "weekly", lastMod);
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid main category: {}", mainCategory, e);
        }
        
        sitemap.append("</urlset>");
        log.info("Category XML sitemap for {} generated successfully", mainCategory);
        return sitemap.toString();
    }
    
    /**
     * Génère un sitemap pour les vendeurs avec des produits actifs
     * @return Contenu du sitemap au format XML
     */
    public String generateSellersSitemap() {
        log.info("Generating sellers XML sitemap");
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        List<User> activeSellers = userRepository.findActiveSellersWithProducts();
        
        for (User seller : activeSellers) {
            String url = frontendBaseUrl + "/sellers/" + seller.getId();
            appendUrl(sitemap, url, "0.7", "weekly");
        }
        
        sitemap.append("</urlset>");
        log.info("Sellers XML sitemap generated successfully with {} sellers", activeSellers.size());
        return sitemap.toString();
    }
    
    /**
     * Ajoute toutes les URLs des produits actifs au sitemap
     */
    private void addProductsToSitemap(StringBuilder sitemap) {
        log.debug("Adding products to sitemap");
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        
        for (Product product : activeProducts) {
            // Utiliser le slug au lieu de l'ID pour l'URL
            String url = product.getSlug() != null && !product.getSlug().isEmpty() 
                ? frontendBaseUrl + "/annonces/" + product.getSlug()
                : frontendBaseUrl + "/annonces/id/" + product.getId();
                
            String lastMod = formatDate(product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt());
            
            // Les produits les plus récents ont une priorité plus élevée
            String priority = calculateProductPriority(product);
            
            appendUrl(sitemap, url, priority, "weekly", lastMod);
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
            appendUrl(sitemap, url, "0.7", "weekly");
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
    
    /**
     * Obtient les sous-catégories pour une catégorie principale
     */
    private List<ProductCategory> getSubcategoriesForMainCategory(ProductCategory mainCategory) {
        List<ProductCategory> result = new ArrayList<>();
        
        switch (mainCategory) {
            case MARKET:
                result.addAll(List.of(
                    ProductCategory.SMARTPHONES_AND_TELEPHONES,
                    ProductCategory.TABLETS_AND_E_BOOKS,
                    ProductCategory.LAPTOPS,
                    ProductCategory.DESKTOP_COMPUTERS,
                    ProductCategory.TELEVISIONS,
                    ProductCategory.ELECTRO_MENAGE,
                    ProductCategory.ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS,
                    ProductCategory.SMARTWATCHES_AND_ACCESSORIES,
                    ProductCategory.AUDIO_AND_HIFI,
                    ProductCategory.COMPUTER_COMPONENTS,
                    ProductCategory.STORAGE_AND_PERIPHERALS,
                    ProductCategory.PRINTERS_AND_SCANNERS,
                    ProductCategory.DRONES_AND_ACCESSORIES,
                    ProductCategory.NETWORK_EQUIPMENT,
                    ProductCategory.SMART_HOME_DEVICES,
                    ProductCategory.GAMING_ACCESSORIES,
                    ProductCategory.PHOTO_AND_VIDEO_EQUIPMENT,
                    ProductCategory.OTHER_CATEGORIES
                ));
                break;
            case VEHICLES:
                result.addAll(List.of(
                    ProductCategory.CARS,
                    ProductCategory.MOTORCYCLES,
                    ProductCategory.BICYCLES,
                    ProductCategory.VEHICLE_PARTS,
                    ProductCategory.TRUCKS_AND_MACHINERY,
                    ProductCategory.BOATS,
                    ProductCategory.OTHER_VEHICLES
                ));
                break;
            case REAL_ESTATE:
                result.addAll(List.of(
                    ProductCategory.REAL_ESTATE_SALES,
                    ProductCategory.APARTMENTS_FOR_SALE,
                    ProductCategory.HOUSES_FOR_SALE,
                    ProductCategory.VILLAS_RIADS_FOR_SALE,
                    ProductCategory.OFFICES_FOR_SALE,
                    ProductCategory.COMMERCIAL_SPACES_FOR_SALE,
                    ProductCategory.LAND_AND_FARMS_FOR_SALE,
                    ProductCategory.OTHER_REAL_ESTATE_FOR_SALE,
                    ProductCategory.REAL_ESTATE_RENTALS,
                    ProductCategory.APARTMENTS_FOR_RENT,
                    ProductCategory.HOUSES_FOR_RENT,
                    ProductCategory.VILLAS_RIADS_FOR_RENT,
                    ProductCategory.OFFICES_FOR_RENT,
                    ProductCategory.COMMERCIAL_SPACES_FOR_RENT,
                    ProductCategory.LAND_AND_FARMS_FOR_RENT,
                    ProductCategory.OTHER_REAL_ESTATE_FOR_RENT
                ));
                break;
            default:
                break;
        }
        
        return result;
    }
    
    /**
     * Renvoie les sous-catégories populaires par catégorie principale
     */
    private Map<String, List<String>> getPopularSubcategories() {
        Map<String, List<String>> result = new HashMap<>();
        
        result.put("MARKET", List.of(
            "SMARTPHONES_AND_TELEPHONES",
            "LAPTOPS",
            "TELEVISIONS", 
            "AUDIO_AND_HIFI"
        ));
        
        result.put("VEHICLES", List.of(
            "CARS",
            "MOTORCYCLES",
            "VEHICLE_PARTS"
        ));
        
        result.put("REAL_ESTATE", List.of(
            "APARTMENTS_FOR_SALE",
            "HOUSES_FOR_SALE",
            "APARTMENTS_FOR_RENT",
            "HOUSES_FOR_RENT"
        ));
        
        return result;
    }

    /**
     * Génère tous les sitemaps nécessaires
     */
    public void generateSitemap() {
        log.info("Starting generation of all sitemaps");

        // Generate the sitemap index
        String sitemapIndex = generateSitemapIndex();
        // Here you would typically save this to a file or push it to a storage system

        // Generate the static sitemap
        String staticSitemap = generateStaticSitemap();
        // Save the static sitemap

        // Generate product sitemaps
        long totalActiveProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        int numSitemapFiles = (int) Math.ceil((double) totalActiveProducts / MAX_URLS_PER_SITEMAP);
        for (int i = 1; i <= numSitemapFiles; i++) {
            String productsSitemap = generateProductsSitemap(i);
            // Save the products sitemap for each page
        }

        // Generate category sitemaps
        for (String mainCategory : List.of("MARKET", "VEHICLES", "REAL_ESTATE")) {
            String categorySitemap = generateCategorySitemap(mainCategory);
            // Save the category sitemap
        }

        // Generate sellers sitemap
        String sellersSitemap = generateSellersSitemap();
        // Save the sellers sitemap

        log.info("All sitemaps generated successfully");
    }
}