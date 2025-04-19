package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.seo.SitemapGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les fonctionnalités SEO comme le sitemap XML
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SeoController {

    private final SitemapGenerator sitemapGenerator;
    
    @Value("${app.backend.url:http://localhost:8081}")
    private String backendBaseUrl;

    /**
     * Endpoint pour accéder au sitemap index XML
     * Accessible à l'URL /sitemap.xml
     *
     * @return Sitemap index XML formaté correctement
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemapIndex() {
        log.info("SEO: Sitemap index requested");
        String sitemapIndex = sitemapGenerator.generateSitemapIndex();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sitemapIndex);
    }
    
    /**
     * Endpoint pour accéder au sitemap statique des pages principales
     * Accessible à l'URL /api/v1/seo/sitemap-static.xml
     *
     * @return Sitemap XML des pages principales
     */
    @GetMapping(value = "/api/v1/seo/sitemap-static.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getStaticSitemap() {
        log.info("SEO: Static sitemap requested");
        String sitemap = sitemapGenerator.generateStaticSitemap();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sitemap);
    }
    
    /**
     * Endpoint pour accéder aux sitemaps des produits (listings) paginés
     * Accessible à l'URL /api/v1/seo/sitemap-listings-{pageNum}.xml
     *
     * @param pageNum Le numéro de la page du sitemap
     * @return Sitemap XML des produits de cette page
     */
    @GetMapping(value = "/api/v1/seo/sitemap-listings-{pageNum}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getProductsSitemap(@PathVariable int pageNum) {
        log.info("SEO: Products sitemap page {} requested", pageNum);
        String sitemap = sitemapGenerator.generateProductsSitemap(pageNum);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sitemap);
    }
    
    /**
     * Endpoint pour accéder aux sitemaps par catégorie
     * Accessible à l'URL /api/v1/seo/sitemap-category-{category}.xml
     *
     * @param category La catégorie principale (market, vehicles, real_estate)
     * @return Sitemap XML des produits de cette catégorie
     */
    @GetMapping(value = "/api/v1/seo/sitemap-category-{category}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getCategorySitemap(@PathVariable String category) {
        log.info("SEO: Category sitemap for {} requested", category);
        String sitemap = sitemapGenerator.generateCategorySitemap(category.toUpperCase());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sitemap);
    }
    
    /**
     * Endpoint pour accéder au sitemap des vendeurs
     * Accessible à l'URL /api/v1/seo/sitemap-sellers.xml
     *
     * @return Sitemap XML des profils vendeurs
     */
    @GetMapping(value = "/api/v1/seo/sitemap-sellers.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSellersSitemap() {
        log.info("SEO: Sellers sitemap requested");
        String sitemap = sitemapGenerator.generateSellersSitemap();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sitemap);
    }

    /**
     * Endpoint pour accéder au fichier robots.txt
     * Accessible à l'URL /robots.txt
     *
     * @return Contenu du fichier robots.txt
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() {
        log.info("SEO: Robots.txt requested");
        
        StringBuilder robotsTxt = new StringBuilder();
        // Règles pour tous les robots
        robotsTxt.append("User-agent: *\n");
        // Protéger les endpoints sensibles et privés
        robotsTxt.append("Disallow: /api/v1/admin/\n");
        robotsTxt.append("Disallow: /api/v1/users/my/\n");
        robotsTxt.append("Disallow: /api/v1/products/my/\n");
        robotsTxt.append("Disallow: /api/v1/auth/\n");
        robotsTxt.append("Disallow: /api/v1/images/upload\n");
        robotsTxt.append("Disallow: /api/v1/contact/\n");
        robotsTxt.append("Disallow: /api/v1/favorites/\n");
        robotsTxt.append("Disallow: /api/v1/notifications/\n");
        robotsTxt.append("Disallow: /api/v1/reports/\n");
        robotsTxt.append("Disallow: /api/v1/reviews/my/\n");
        robotsTxt.append("Disallow: /api/v1/preferences/\n\n");
        
        // Optimisations pour les crawlers de Google
        robotsTxt.append("# Optimisations for Google bots\n");
        robotsTxt.append("User-agent: Googlebot\n");
        robotsTxt.append("Allow: /\n\n");
        
        robotsTxt.append("User-agent: Googlebot-Image\n");
        robotsTxt.append("Allow: /api/v1/images/\n\n");
        
        robotsTxt.append("User-agent: Googlebot-News\n");
        robotsTxt.append("Allow: /api/v1/products/\n\n");
        
        robotsTxt.append("User-agent: Googlebot-Mobile\n");
        robotsTxt.append("Allow: /\n\n");
        
        // Limiter le taux de crawl pour les robots agressifs
        robotsTxt.append("# Rate limiting\n");
        robotsTxt.append("User-agent: AhrefsBot\n");
        robotsTxt.append("Crawl-delay: 10\n\n");
        
        robotsTxt.append("User-agent: MJ12bot\n");
        robotsTxt.append("Crawl-delay: 10\n\n");
        
        // Lien vers le sitemap principal
        robotsTxt.append("# Sitemap\n");
        robotsTxt.append("Sitemap: ").append(backendBaseUrl).append("/sitemap.xml\n");
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(robotsTxt.toString());
    }
}