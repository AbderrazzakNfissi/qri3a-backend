package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.SearchTerm;
import my.project.qri3a.repositories.SearchTermRepository;
import my.project.qri3a.services.SearchTermService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des termes de recherche
 * Utilisé pour l'analyse des tendances de recherche et l'amélioration du SEO
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SearchTermServiceImpl implements SearchTermService {

    private final SearchTermRepository searchTermRepository;

    @Override
    public void recordSearchTerm(String term, String category, String location) {
        if (term == null || term.trim().isEmpty()) {
            return; // Ne pas traiter les recherches vides
        }

        // Normaliser le terme de recherche (minuscules, trim)
        term = term.toLowerCase().trim();
        
        log.debug("Enregistrement du terme de recherche: '{}', categorie: {}, localisation: {}", term, category, location);
        
        Optional<SearchTerm> existingTerm;
        
        // Rechercher par terme et catégorie si la catégorie est fournie
        if (category != null && !category.isEmpty()) {
            existingTerm = searchTermRepository.findByTermAndCategory(term, category);
        } else {
            existingTerm = searchTermRepository.findByTerm(term);
        }

        if (existingTerm.isPresent()) {
            // Incrémenter le compteur d'un terme existant
            SearchTerm searchTerm = existingTerm.get();
            searchTerm.setCount(searchTerm.getCount() + 1);
            searchTerm.setLastSearched(new Date());
            searchTerm.setLocation(location); // Mettre à jour la localisation si nécessaire
            
            searchTermRepository.save(searchTerm);
            log.debug("Terme de recherche mis à jour: {}, compteur: {}", term, searchTerm.getCount());
        } else {
            // Créer un nouveau terme de recherche
            SearchTerm newSearchTerm = SearchTerm.builder()
                .term(term)
                .count(1)
                .lastSearched(new Date())
                .category(category)
                .location(location)
                .build();
                
            searchTermRepository.save(newSearchTerm);
            log.debug("Nouveau terme de recherche enregistré: {}", term);
        }
    }

    @Override
    public List<SearchTerm> getTopSearchTerms(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return searchTermRepository.findTopSearchTerms(pageable);
    }

    @Override
    public List<SearchTerm> getTopSearchTermsByCategory(String category, int limit) {
        if (category == null || category.isEmpty()) {
            return getTopSearchTerms(limit);
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        return searchTermRepository.findTopSearchTermsByCategory(category, pageable);
    }
}