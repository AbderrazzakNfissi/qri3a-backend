package my.project.qri3a.services;

import java.util.Map;

public interface TranslationService {
    /**
     * Traduit une clé dans la langue spécifiée
     * @param key La clé à traduire
     * @param lang Le code de langue (fr, en, arm)
     * @return La chaîne traduite ou la clé elle-même si non trouvée
     */
    String translate(String key, String lang);

    /**
     * Traduit une clé dans la langue spécifiée avec des paramètres à substituer
     * @param key La clé à traduire
     * @param lang Le code de langue (fr, en, arm)
     * @param params Les paramètres à substituer dans la chaîne traduite
     * @return La chaîne traduite avec les paramètres substitués
     */
    String translate(String key, String lang, Map<String, Object> params);
}