package my.project.qri3a.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Entité qui représente un terme de recherche utilisé par les utilisateurs
 * Permet de suivre les recherches populaires et d'améliorer le SEO
 */
@Entity
@Table(name = "search_terms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String term;
    
    @Column(nullable = false)
    private int count;
    
    @Column(nullable = false)
    private Date lastSearched;
    
    @Column(nullable = true)
    private String category;
    
    @Column(nullable = true)
    private String location;
}