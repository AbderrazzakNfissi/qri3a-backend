package my.project.qri3a.services;

import java.util.Date;
import java.util.List;

import my.project.qri3a.entities.SearchTerm;

/**
 * Interface pour le service de gestion des termes de recherche
 * Utilisé pour l'analyse SEO et les tendances de recherche
 */
public interface SearchTermService {
    
    /**
     * Enregistre un nouveau terme de recherche ou incrémente son compteur s'il existe déjà
     * 
     * @param term Le terme recherché
     * @param category La catégorie associée, peut être null
     * @param location La localisation associée, peut être null
     */
    void recordSearchTerm(String term, String category, String location);
    
    /**
     * Récupère les N termes de recherche les plus populaires
     * 
     * @param limit Nombre maximum de termes à récupérer
     * @return Liste des termes les plus recherchés
     */
    List<SearchTerm> getTopSearchTerms(int limit);
    
    /**
     * Récupère les N termes de recherche les plus populaires dans une catégorie spécifique
     * 
     * @param category La catégorie à filtrer
     * @param limit Nombre maximum de termes à récupérer
     * @return Liste des termes les plus recherchés dans la catégorie
     */
    List<SearchTerm> getTopSearchTermsByCategory(String category, int limit);
}