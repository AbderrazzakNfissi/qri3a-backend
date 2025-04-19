package my.project.qri3a.seo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Planificateur pour la régénération automatique du sitemap XML
 * Permet de maintenir le sitemap à jour pour les moteurs de recherche
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SitemapScheduler {

    private final SitemapGenerator sitemapGenerator;
    
    /**
     * Régénère le sitemap chaque jour à minuit
     * La valeur cron "0 0 0 * * ?" signifie : secondes(0) minutes(0) heures(0) tous-les-jours(*) tous-les-mois(*) tous-les-jours-de-la-semaine(?)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void regenerateSitemapDaily() {
        log.info("Régénération planifiée du sitemap XML");
        sitemapGenerator.generateSitemap();
        log.info("Sitemap XML régénéré avec succès");
    }
}