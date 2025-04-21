package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
import my.project.qri3a.dtos.responses.ScamAttachmentResponseDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.entities.ScamAttachment;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.ScamAttachmentMapper;
import my.project.qri3a.services.ScamAttachmentService;
import my.project.qri3a.services.ScamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scams")
@RequiredArgsConstructor
@Slf4j
public class ScamController {

    private final ScamService scamService;
    private final ScamAttachmentService scamAttachmentService;
    private final ScamAttachmentMapper scamAttachmentMapper;

    /**
     * Endpoint pour créer un nouveau signalement d'arnaque (toujours anonyme).
     */
    @PostMapping
    public ResponseEntity<ScamResponseDTO> reportScam(
            @Valid @RequestBody ScamReportRequestDTO dto
    ) throws ResourceNotFoundException {
        log.info("Controller: Creating new scam report for product with identifier: {}", dto.getProductIdentifier());
        
        ScamResponseDTO response = scamService.reportScam(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Endpoint pour uploader une pièce jointe à un signalement d'arnaque.
     */
    @PostMapping(value = "/{scamId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScamAttachmentResponseDTO> uploadAttachment(
            @PathVariable("scamId") UUID scamId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "other") String attachmentType
    ) throws ResourceNotFoundException, IOException {
        log.info("Controller: Uploading attachment for scam report with ID: {}", scamId);
        
        ScamAttachment attachment = scamAttachmentService.uploadAttachment(scamId, file, attachmentType);
        ScamAttachmentResponseDTO response = scamAttachmentMapper.toDTO(attachment);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Endpoint pour uploader plusieurs pièces jointes à un signalement d'arnaque.
     */
    @PostMapping(value = "/{scamId}/attachments/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScamAttachmentResponseDTO>> uploadAttachments(
            @PathVariable("scamId") UUID scamId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "type", defaultValue = "other") String attachmentType
    ) throws ResourceNotFoundException, IOException {
        log.info("Controller: Uploading {} attachments for scam report with ID: {}", files.size(), scamId);
        
        List<ScamAttachment> attachments = scamAttachmentService.uploadAttachments(scamId, files, attachmentType);
        List<ScamAttachmentResponseDTO> response = scamAttachmentMapper.toDTOList(attachments);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Endpoint pour récupérer toutes les pièces jointes d'un signalement d'arnaque.
     */
    @GetMapping("/{scamId}/attachments")
    public ResponseEntity<List<ScamAttachmentResponseDTO>> getAttachments(
            @PathVariable("scamId") UUID scamId
    ) throws ResourceNotFoundException {
        log.info("Controller: Fetching attachments for scam report with ID: {}", scamId);
        
        List<ScamAttachment> attachments = scamAttachmentService.getAttachmentsByScamId(scamId);
        List<ScamAttachmentResponseDTO> response = scamAttachmentMapper.toDTOList(attachments);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint pour supprimer une pièce jointe d'un signalement d'arnaque.
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable("attachmentId") UUID attachmentId
    ) throws ResourceNotFoundException {
        log.info("Controller: Deleting attachment with ID: {}", attachmentId);
        
        scamAttachmentService.deleteAttachment(attachmentId);
        
        return ResponseEntity.noContent().build();
    }


}