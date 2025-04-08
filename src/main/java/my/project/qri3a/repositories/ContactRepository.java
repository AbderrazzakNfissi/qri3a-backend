package my.project.qri3a.repositories;

import my.project.qri3a.entities.Contact;
import my.project.qri3a.enums.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Page<Contact> findAll(Pageable pageable);

    Page<Contact> findByStatus(ContactStatus status, Pageable pageable);

    Page<Contact> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Contact> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Contact> findByUserId(UUID userId, Pageable pageable);

    List<Contact> findTop5ByOrderByCreatedAtDesc();

    int countByStatus(ContactStatus status);
}