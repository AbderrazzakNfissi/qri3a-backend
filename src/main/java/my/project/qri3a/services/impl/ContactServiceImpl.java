package my.project.qri3a.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ContactRequestDTO;
import my.project.qri3a.dtos.responses.ContactResponseDTO;
import my.project.qri3a.entities.Contact;
import my.project.qri3a.enums.ContactStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.ContactMapper;
import my.project.qri3a.repositories.ContactRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ContactService;
import my.project.qri3a.services.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactMapper contactMapper;
    private final EmailService emailService;

    @Override
    public ContactResponseDTO submitContact(ContactRequestDTO contactRequestDTO, HttpServletRequest request, Authentication authentication) {
        log.info("Service: Submitting contact form from: {}", contactRequestDTO.getEmail());

        // Créer l'entité Contact
        Contact contact = contactMapper.toEntity(contactRequestDTO);

        // Ajouter les informations de l'utilisateur si connecté
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            userRepository.findByEmail(authentication.getName()).ifPresent(contact::setUser);
        }

        // Ajouter l'adresse IP et User-Agent
        contact.setIpAddress(getClientIp(request));
        contact.setUserAgent(request.getHeader("User-Agent"));

        // Enregistrer dans la base de données
        Contact savedContact = contactRepository.save(contact);
        log.info("Service: Contact form submitted with ID: {}", savedContact.getId());

        // Envoyer notification par email (administrateur)
        emailService.sendContactNotificationToAdmin(savedContact);

        // Envoyer confirmation par email (utilisateur)
        emailService.sendContactConfirmationEmail(savedContact);

        return contactMapper.toDTO(savedContact);
    }

    @Override
    public Page<Contact> getAllContacts(Pageable pageable) {
        log.info("Service: Fetching all contacts with pagination");
        return contactRepository.findAll(pageable);
    }

    @Override
    public Page<Contact> getContactsByStatus(ContactStatus status, Pageable pageable) {
        log.info("Service: Fetching contacts by status: {}", status);
        return contactRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Contact> searchContacts(String query, Pageable pageable) {
        log.info("Service: Searching contacts with query: {}", query);
        if (query.contains("@")) {
            return contactRepository.findByEmailContainingIgnoreCase(query, pageable);
        } else {
            return contactRepository.findByNameContainingIgnoreCase(query, pageable);
        }
    }

    @Override
    public Page<Contact> getContactsByUser(UUID userId, Pageable pageable) {
        log.info("Service: Fetching contacts by user ID: {}", userId);
        return contactRepository.findByUserId(userId, pageable);
    }

    @Override
    public Optional<Contact> getContactById(UUID contactId) {
        log.info("Service: Fetching contact by ID: {}", contactId);
        return contactRepository.findById(contactId);
    }

    @Override
    public Contact updateContactStatus(UUID contactId, ContactStatus status) throws ResourceNotFoundException {
        log.info("Service: Updating contact status to {} for ID: {}", status, contactId);
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> {
                    log.warn("Service: Contact not found with ID: {}", contactId);
                    return new ResourceNotFoundException("Contact not found with ID " + contactId);
                });

        contact.setStatus(status);
        return contactRepository.save(contact);
    }

    @Override
    public void deleteContact(UUID contactId) throws ResourceNotFoundException {
        log.info("Service: Deleting contact with ID: {}", contactId);
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> {
                    log.warn("Service: Contact not found with ID: {}", contactId);
                    return new ResourceNotFoundException("Contact not found with ID " + contactId);
                });

        contactRepository.delete(contact);
        log.info("Service: Contact deleted with ID: {}", contactId);
    }

    @Override
    public int countContactsByStatus(ContactStatus status) {
        return contactRepository.countByStatus(status);
    }


    private String getClientIp(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}