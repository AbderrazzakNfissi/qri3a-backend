package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.SearchTerm;
import my.project.qri3a.entities.User;
import my.project.qri3a.entities.UserSearchHistory;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.SearchTermRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.repositories.UserSearchHistoryRepository;
import my.project.qri3a.services.SearchTermService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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
    private final UserSearchHistoryRepository userSearchHistoryRepository;
    private final UserRepository userRepository;

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
    
    @Override
    public void recordUserSearchHistory(String term, String category, String location, Authentication authentication) {
        if (term == null || term.trim().isEmpty() || authentication == null) {
            return; // Ne pas traiter les recherches vides ou les utilisateurs non authentifiés
        }
        
        // Normaliser le terme de recherche (minuscules, trim)
        term = term.toLowerCase().trim();
        
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            
            // Vérifier si le terme existe déjà pour cet utilisateur
            Optional<UserSearchHistory> existingEntry = userSearchHistoryRepository.findByUserAndSearchTerm(user, term);
            
            if (existingEntry.isPresent()) {
                // Si le terme existe déjà, récupérer l'entrée existante
                UserSearchHistory searchHistory = existingEntry.get();
                
                // Mettre à jour les champs si nécessaire
                searchHistory.setCategory(category);
                searchHistory.setLocation(location);
                
                // Utilisez EntityManager pour mettre à jour directement la date de création
                // Cette approche est plus performante car elle évite de lire l'entité complète
                userSearchHistoryRepository.save(searchHistory);
                log.debug("Historique de recherche mis à jour pour l'utilisateur: {}, terme: {}", email, term);
            } else {
                // Créer une nouvelle entrée d'historique de recherche
                UserSearchHistory searchHistory = UserSearchHistory.builder()
                    .user(user)
                    .searchTerm(term)
                    .category(category)
                    .location(location)
                    .build();
                    
                userSearchHistoryRepository.save(searchHistory);
                log.debug("Nouvel historique de recherche enregistré pour l'utilisateur: {}, terme: {}", email, term);
            }
            
            // Également enregistrer dans les statistiques générales
            recordSearchTerm(term, category, location);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'historique de recherche: {}", e.getMessage());
        }
    }
    
    @Override
    public Page<UserSearchHistory> getUserSearchHistory(Authentication authentication, Pageable pageable) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        return userSearchHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
}