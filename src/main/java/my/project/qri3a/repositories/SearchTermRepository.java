package my.project.qri3a.repositories;

import my.project.qri3a.entities.SearchTerm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour interagir avec la table des termes de recherche
 * Utilisé pour le suivi et l'analyse des recherches populaires
 */
@Repository
public interface SearchTermRepository extends JpaRepository<SearchTerm, UUID> {
    
    /**
     * Trouve un terme de recherche par son texte exact
     */
    Optional<SearchTerm> findByTerm(String term);
    
    /**
     * Trouve un terme de recherche par son texte exact et sa catégorie
     */
    Optional<SearchTerm> findByTermAndCategory(String term, String category);
    
    /**
     * Trouve tous les termes de recherche par leur texte exact (peut retourner plusieurs résultats si les catégories diffèrent)
     */
    List<SearchTerm> findAllByTerm(String term);

    /**
     * Trouve les termes de recherche les plus populaires
     */
    @Query("SELECT s FROM SearchTerm s ORDER BY s.count DESC")
    List<SearchTerm> findTopSearchTerms(Pageable pageable);
    
    /**
     * Trouve les termes de recherche les plus populaires dans une catégorie spécifique
     */
    @Query("SELECT s FROM SearchTerm s WHERE s.category = :category ORDER BY s.count DESC")
    List<SearchTerm> findTopSearchTermsByCategory(@Param("category") String category, Pageable pageable);
}