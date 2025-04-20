package my.project.qri3a.mappers;

import my.project.qri3a.dtos.responses.UserSearchHistoryDTO;
import my.project.qri3a.entities.UserSearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir les entités UserSearchHistory en DTOs
 */
@Component
public class UserSearchHistoryMapper {

    /**
     * Convertit une entité UserSearchHistory en DTO
     * @param entity L'entité à convertir
     * @return Le DTO correspondant
     */
    public UserSearchHistoryDTO toDTO(UserSearchHistory entity) {
        if (entity == null) {
            return null;
        }
        
        return UserSearchHistoryDTO.builder()
                .id(entity.getId())
                .searchTerm(entity.getSearchTerm())
                .category(entity.getCategory())
                .location(entity.getLocation())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    /**
     * Convertit une page d'entités UserSearchHistory en page de DTOs
     * @param page La page d'entités à convertir
     * @return La page de DTOs correspondante
     */
    public Page<UserSearchHistoryDTO> toDTOPage(Page<UserSearchHistory> page) {
        return page.map(this::toDTO);
    }
}