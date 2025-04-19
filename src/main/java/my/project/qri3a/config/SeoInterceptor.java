package my.project.qri3a.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepteur qui ajoute des en-têtes HTTP pour améliorer le SEO
 * et le contrôle du cache pour les API publiques
 */
@Component
@Slf4j
public class SeoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Configurer les en-têtes SEO uniquement pour les APIs accessibles publiquement
        String requestURI = request.getRequestURI();

        // On applique les en-têtes SEO uniquement aux endpoints publics
        // Comme les produits, les catégories, les recherches, etc.
        if (isPublicApiEndpoint(requestURI)) {
            log.debug("Adding SEO headers for endpoint: {}", requestURI);
            
            // Cache-Control
            // Permet au navigateur de mettre en cache la réponse pendant une durée définie (en secondes)
            // Les moteurs de recherche peuvent utiliser ce header pour optimiser le crawling
            response.setHeader("Cache-Control", "public, max-age=300, must-revalidate");
            
            // Permet l'indexation par défaut, peut être spécifié différemment pour des endpoints spécifiques
            if (shouldAllowIndexing(requestURI)) {
                response.setHeader("X-Robots-Tag", "index, follow");
            } else {
                response.setHeader("X-Robots-Tag", "noindex, nofollow");
            }
        } else {
            // Pour les endpoints qui ne sont pas publics, désactivez l'indexation
            response.setHeader("X-Robots-Tag", "noindex, nofollow");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
        
        return true;
    }

    /**
     * Détermine si un endpoint API fait partie des APIs publiques qui devraient
     * être optimisées pour le SEO
     */
    private boolean isPublicApiEndpoint(String uri) {
        return uri.matches("/api/v1/products(/.*)?") || // Tous les endpoints de produits
               uri.matches("/api/v1/users/seller-profile/.*") || // Profils des vendeurs
               uri.startsWith("/api/v1/reviews") ||
               uri.matches("/api/v1/products/search.*");
    }

    /**
     * Détermine si un endpoint spécifique devrait être indexé par les moteurs de recherche
     */
    private boolean shouldAllowIndexing(String uri) {
        // Par exemple, nous voulons indexer les pages de produits individuels
        // mais pas les résultats de recherche
        return uri.matches("/api/v1/products/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") || // Détails d'un produit (UUID)
               uri.matches("/api/v1/users/seller-profile/.*"); // Profils des vendeurs
    }
}