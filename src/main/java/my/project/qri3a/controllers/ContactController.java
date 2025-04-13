package my.project.qri3a.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ContactRequestDTO;
import my.project.qri3a.dtos.responses.ContactResponseDTO;
import my.project.qri3a.entities.Contact;
import my.project.qri3a.enums.ContactStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.ContactMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ContactService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;
    private final ContactMapper contactMapper;

    /**
     * POST /api/v1/contacts
     * Soumettre un nouveau message de contact
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponseDTO>> submitContact(
            @Valid @RequestBody ContactRequestDTO contactRequestDTO,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("Controller: Submitting contact form from: {}", contactRequestDTO.getEmail());

        ContactResponseDTO responseDTO = contactService.submitContact(contactRequestDTO, request, authentication);

        ApiResponse<ContactResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "Votre message a été envoyé avec succès. Nous vous répondrons dans les plus brefs délais.",
                HttpStatus.CREATED.value()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/contacts
     * Récupérer tous les messages de contact (admin seulement)
     */
    @GetMapping
   
    public ResponseEntity<ApiResponse<Page<ContactResponseDTO>>> getAllContacts(Pageable pageable) {
        log.info("Controller: Fetching all contacts with pagination");

        Page<Contact> contactsPage = contactService.getAllContacts(pageable);
        Page<ContactResponseDTO> dtoPage = contactsPage.map(contactMapper::toDTO);

        ApiResponse<Page<ContactResponseDTO>> response = new ApiResponse<>(
                dtoPage,
                "Contacts fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/contacts/status/{status}
     * Récupérer les messages de contact par statut (admin seulement)
     */
    @GetMapping("/status/{status}")
   
    public ResponseEntity<ApiResponse<Page<ContactResponseDTO>>> getContactsByStatus(
            @PathVariable ContactStatus status,
            Pageable pageable) {

        log.info("Controller: Fetching contacts by status: {}", status);

        Page<Contact> contactsPage = contactService.getContactsByStatus(status, pageable);
        Page<ContactResponseDTO> dtoPage = contactsPage.map(contactMapper::toDTO);

        ApiResponse<Page<ContactResponseDTO>> response = new ApiResponse<>(
                dtoPage,
                "Contacts with status " + status + " fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/contacts/search?query=
     * Rechercher des messages de contact (admin seulement)
     */
    @GetMapping("/search")
   
    public ResponseEntity<ApiResponse<Page<ContactResponseDTO>>> searchContacts(
            @RequestParam String query,
            Pageable pageable) {

        log.info("Controller: Searching contacts with query: {}", query);

        Page<Contact> contactsPage = contactService.searchContacts(query, pageable);
        Page<ContactResponseDTO> dtoPage = contactsPage.map(contactMapper::toDTO);

        ApiResponse<Page<ContactResponseDTO>> response = new ApiResponse<>(
                dtoPage,
                "Search results for query: " + query,
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

//    /**
//     * GET /api/v1/contacts/user/{userId}
//     * Récupérer les messages de contact par utilisateur (admin seulement)
//     */
//    @GetMapping("/user/{userId}")
//
//    public ResponseEntity<ApiResponse<Page<ContactResponseDTO>>> getContactsByUser(
//            @PathVariable UUID userId,
//            Pageable pageable) {
//
//        log.info("Controller: Fetching contacts by user ID: {}", userId);
//
//        Page<Contact> contactsPage = contactService.getContactsByUser(userId, pageable);
//        Page<ContactResponseDTO> dtoPage = contactsPage.map(contactMapper::toDTO);
//
//        ApiResponse<Page<ContactResponseDTO>> response = new ApiResponse<>(
//                dtoPage,
//                "Contacts for user " + userId + " fetched successfully.",
//                HttpStatus.OK.value()
//        );
//
//        return ResponseEntity.ok(response);
//    }

//    /**
//     * GET /api/v1/contacts/{id}
//     * Récupérer un message de contact par ID (admin seulement)
//     */
//    @GetMapping("/{id}")
//
//    public ResponseEntity<ApiResponse<ContactResponseDTO>> getContactById(@PathVariable UUID id) throws ResourceNotFoundException {
//        log.info("Controller: Fetching contact with ID: {}", id);
//
//        Contact contact = contactService.getContactById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID " + id));
//
//        ContactResponseDTO responseDTO = contactMapper.toDTO(contact);
//
//        ApiResponse<ContactResponseDTO> response = new ApiResponse<>(
//                responseDTO,
//                "Contact fetched successfully.",
//                HttpStatus.OK.value()
//        );
//
//        return ResponseEntity.ok(response);
//    }

    /**
     * PATCH /api/v1/contacts/{id}/status
     * Mettre à jour le statut d'un message de contact (admin seulement)
     */
    @PatchMapping("/{id}/status")
   
    public ResponseEntity<ApiResponse<ContactResponseDTO>> updateContactStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, ContactStatus> statusMap) throws ResourceNotFoundException {

        ContactStatus newStatus = statusMap.get("status");
        if (newStatus == null) {
            throw new IllegalArgumentException("Status value is required");
        }

        log.info("Controller: Updating contact status to {} for ID: {}", newStatus, id);

        Contact updatedContact = contactService.updateContactStatus(id, newStatus);
        ContactResponseDTO responseDTO = contactMapper.toDTO(updatedContact);

        ApiResponse<ContactResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "Contact status updated successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/contacts/{id}
     * Supprimer un message de contact (admin seulement)
     */
    @DeleteMapping("/{id}")
   
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable UUID id) throws ResourceNotFoundException {
        log.info("Controller: Deleting contact with ID: {}", id);

        contactService.deleteContact(id);

        ApiResponse<Void> response = new ApiResponse<>(
                null,
                "Contact deleted successfully.",
                HttpStatus.NO_CONTENT.value()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }


    /**
     * GET /api/v1/contacts/count/new
     * Compter les nouveaux messages de contact (admin seulement)
     */
    @GetMapping("/count/new")
   
    public ResponseEntity<ApiResponse<Integer>> getNewContactsCount() {
        log.info("Controller: Counting new contacts");

        int count = contactService.countContactsByStatus(ContactStatus.NEW);

        ApiResponse<Integer> response = new ApiResponse<>(
                count,
                "New contacts count fetched successfully.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }
}