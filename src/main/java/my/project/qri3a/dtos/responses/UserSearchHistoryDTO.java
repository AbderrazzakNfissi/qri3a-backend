package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour représenter une entrée d'historique de recherche utilisateur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchHistoryDTO {
    
    private UUID id;
    private String searchTerm;
    private String category;
    private String location;
    private LocalDateTime createdAt;
}