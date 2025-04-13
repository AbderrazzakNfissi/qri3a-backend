package my.project.qri3a.services;

import jakarta.servlet.http.HttpServletRequest;
import my.project.qri3a.dtos.requests.ContactRequestDTO;
import my.project.qri3a.dtos.responses.ContactResponseDTO;
import my.project.qri3a.entities.Contact;
import my.project.qri3a.enums.ContactStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactService {

    ContactResponseDTO submitContact(ContactRequestDTO contactRequestDTO, HttpServletRequest request, Authentication authentication);

    Page<Contact> getAllContacts(Pageable pageable);

    Page<Contact> getContactsByStatus(ContactStatus status, Pageable pageable);

    Page<Contact> searchContacts(String query, Pageable pageable);

    Page<Contact> getContactsByUser(UUID userId, Pageable pageable);

    Optional<Contact> getContactById(UUID contactId);

    Contact updateContactStatus(UUID contactId, ContactStatus status) throws ResourceNotFoundException;

    void deleteContact(UUID contactId) throws ResourceNotFoundException;

    int countContactsByStatus(ContactStatus status);
}