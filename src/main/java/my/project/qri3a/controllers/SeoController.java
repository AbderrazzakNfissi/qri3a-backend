package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.seo.SitemapGenerator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les fonctionnalités SEO comme le sitemap XML
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class SeoController {

    private final SitemapGenerator sitemapGenerator;

    /**
     * Endpoint pour accéder au sitemap XML
     * Accessible à l'URL /sitemap.xml
     *
     * @return Sitemap XML formaté correctement
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        log.info("SEO: Sitemap requested");
        String sitemap = sitemapGenerator.generateSitemap();
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
        robotsTxt.append("User-agent: *\n");
        robotsTxt.append("Disallow: /api/v1/admin/\n");
        robotsTxt.append("Disallow: /api/v1/users/my/\n");
        robotsTxt.append("Disallow: /api/v1/products/my/\n");
        robotsTxt.append("Disallow: /api/v1/auth/\n\n");
        robotsTxt.append("Sitemap: https://yourdomain.com/sitemap.xml\n");
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(robotsTxt.toString());
    }
}