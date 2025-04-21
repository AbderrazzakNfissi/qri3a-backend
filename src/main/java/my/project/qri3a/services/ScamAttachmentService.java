package my.project.qri3a.services;

import my.project.qri3a.entities.ScamAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ScamAttachmentService {
    
    /**
     * Télécharge une pièce jointe pour un signalement d'arnaque.
     *
     * @param scamId ID du signalement d'arnaque
     * @param file Fichier à télécharger
     * @param attachmentType Type de pièce jointe
     * @return L'entité ScamAttachment créée
     * @throws IOException En cas d'erreur lors de l'upload
     */
    ScamAttachment uploadAttachment(UUID scamId, MultipartFile file, String attachmentType) throws IOException;
    
    /**
     * Télécharge plusieurs pièces jointes pour un signalement d'arnaque.
     *
     * @param scamId ID du signalement d'arnaque
     * @param files Liste des fichiers à télécharger
     * @param attachmentType Type de pièce jointe
     * @return Liste des entités ScamAttachment créées
     * @throws IOException En cas d'erreur lors de l'upload
     */
    List<ScamAttachment> uploadAttachments(UUID scamId, List<MultipartFile> files, String attachmentType) throws IOException;
    
    /**
     * Récupère toutes les pièces jointes associées à un signalement.
     *
     * @param scamId ID du signalement d'arnaque
     * @return Liste des pièces jointes
     */
    List<ScamAttachment> getAttachmentsByScamId(UUID scamId);
    
    /**
     * Supprime une pièce jointe.
     *
     * @param attachmentId ID de la pièce jointe à supprimer
     */
    void deleteAttachment(UUID attachmentId);
    
    /**
     * Supprime toutes les pièces jointes associées à un signalement.
     *
     * @param scamId ID du signalement d'arnaque
     */
    void deleteAttachmentsByScamId(UUID scamId);
}