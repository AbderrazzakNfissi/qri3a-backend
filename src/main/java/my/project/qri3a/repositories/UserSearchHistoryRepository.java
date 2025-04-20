package my.project.qri3a.repositories;

import my.project.qri3a.entities.User;
import my.project.qri3a.entities.UserSearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour gérer l'historique de recherche des utilisateurs
 */
@Repository
public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, UUID> {
    
    /**
     * Récupère l'historique de recherche d'un utilisateur spécifique, trié par date de création
     * @param user L'utilisateur dont on veut l'historique
     * @param pageable Paramètres de pagination
     * @return Page contenant l'historique de recherche
     */
    Page<UserSearchHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * Trouve une entrée d'historique par utilisateur et terme de recherche
     * @param user L'utilisateur
     * @param searchTerm Le terme recherché
     * @return L'entrée d'historique si elle existe
     */
    Optional<UserSearchHistory> findByUserAndSearchTerm(User user, String searchTerm);
}