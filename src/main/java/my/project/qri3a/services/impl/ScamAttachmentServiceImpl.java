package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.Scam;
import my.project.qri3a.entities.ScamAttachment;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.ScamAttachmentRepository;
import my.project.qri3a.repositories.ScamRepository;
import my.project.qri3a.services.S3Service;
import my.project.qri3a.services.ScamAttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ScamAttachmentServiceImpl implements ScamAttachmentService {

    private final ScamRepository scamRepository;
    private final ScamAttachmentRepository attachmentRepository;
    private final S3Service s3Service;

    @Override
    public ScamAttachment uploadAttachment(UUID scamId, MultipartFile file, String attachmentType) throws IOException {
        log.info("Uploading attachment for scam with ID: {}", scamId);
        
        // Vérifier si le signalement existe
        Scam scam = scamRepository.findById(scamId)
                .orElseThrow(() -> new ResourceNotFoundException("Scam report not found with ID " + scamId));

        // Générer un nom de fichier unique basé sur l'UUID pour éviter les conflits
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String uniqueFileName = "scams/" + scamId + "/" + UUID.randomUUID() + fileExtension;

        // Télécharger le fichier sur S3
        String fileUrl = s3Service.uploadFile(file, uniqueFileName);

        // Créer et sauvegarder l'entité ScamAttachment
        ScamAttachment attachment = ScamAttachment.builder()
                .scam(scam)
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .type(ScamAttachment.AttachmentType.fromCode(attachmentType))
                .build();

        ScamAttachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Attachment uploaded successfully with ID: {}", savedAttachment.getId());

        return savedAttachment;
    }

    @Override
    public List<ScamAttachment> uploadAttachments(UUID scamId, List<MultipartFile> files, String attachmentType) throws IOException {
        log.info("Uploading {} attachments for scam with ID: {}", files.size(), scamId);
        
        List<ScamAttachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            attachments.add(uploadAttachment(scamId, file, attachmentType));
        }
        
        return attachments;
    }

    @Override
    public List<ScamAttachment> getAttachmentsByScamId(UUID scamId) {
        log.info("Fetching attachments for scam with ID: {}", scamId);
        
        // Vérifier si le signalement existe
        if (!scamRepository.existsById(scamId)) {
            throw new ResourceNotFoundException("Scam report not found with ID " + scamId);
        }
        
        return attachmentRepository.findByScamId(scamId);
    }

    @Override
    public void deleteAttachment(UUID attachmentId) {
        log.info("Deleting attachment with ID: {}", attachmentId);
        
        // Récupérer la pièce jointe
        ScamAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with ID " + attachmentId));
        
        // Extraire le nom du fichier S3 à partir de l'URL
        String s3Key = attachment.getFileUrl().substring(attachment.getFileUrl().indexOf("scams/"));
        
        // Supprimer le fichier de S3
        try {
            s3Service.deleteFile(s3Key);
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
        }
        
        // Supprimer l'entité de la base de données
        attachmentRepository.deleteById(attachmentId);
        log.info("Attachment deleted successfully with ID: {}", attachmentId);
    }

    @Override
    public void deleteAttachmentsByScamId(UUID scamId) {
        log.info("Deleting all attachments for scam with ID: {}", scamId);
        
        // Récupérer toutes les pièces jointes pour le signalement
        List<ScamAttachment> attachments = attachmentRepository.findByScamId(scamId);
        
        // Supprimer chaque fichier de S3
        for (ScamAttachment attachment : attachments) {
            String s3Key = attachment.getFileUrl().substring(attachment.getFileUrl().indexOf("scams/"));
            try {
                s3Service.deleteFile(s3Key);
            } catch (Exception e) {
                log.error("Error deleting file from S3: {}", e.getMessage());
            }
        }
        
        // Supprimer toutes les pièces jointes de la base de données
        attachmentRepository.deleteByScamId(scamId);
        log.info("All attachments deleted successfully for scam with ID: {}", scamId);
    }
}