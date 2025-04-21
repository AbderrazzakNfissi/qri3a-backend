package my.project.qri3a.repositories;

import my.project.qri3a.entities.ScamAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScamAttachmentRepository extends JpaRepository<ScamAttachment, UUID> {
    
    /**
     * Trouve toutes les pièces jointes associées à un signalement.
     *
     * @param scamId L'ID du signalement
     * @return Liste des pièces jointes
     */
    List<ScamAttachment> findByScamId(UUID scamId);
    
    /**
     * Supprime toutes les pièces jointes associées à un signalement.
     *
     * @param scamId L'ID du signalement
     */
    void deleteByScamId(UUID scamId);
}