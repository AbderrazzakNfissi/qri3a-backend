package my.project.qri3a.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité qui représente l'historique de recherche d'un utilisateur spécifique
 */
@Entity
@Table(
    name = "user_search_history", 
    indexes = {
        @Index(name = "idx_user_search_term", columnList = "user_id,search_term")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    
    @Column(nullable = false)
    private String searchTerm;
    
    @Column(nullable = true)
    private String category;
    
    @Column(nullable = true)
    private String location;

    @CreationTimestamp
    private LocalDateTime createdAt;
}