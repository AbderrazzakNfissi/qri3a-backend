package my.project.qri3a.mappers;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ContactRequestDTO;
import my.project.qri3a.dtos.responses.ContactResponseDTO;
import my.project.qri3a.entities.Contact;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.ContactStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Component
public class ContactMapper {

    public Contact toEntity(ContactRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Contact contact = new Contact();
        BeanUtils.copyProperties(dto, contact);
        // Définir le statut par défaut sur NEW
        contact.setStatus(ContactStatus.NEW);
        return contact;
    }

    public ContactResponseDTO toDTO(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactResponseDTO dto = new ContactResponseDTO();
        BeanUtils.copyProperties(contact, dto);

        // Formater les dates comme dans UserMapper
        LocalDateTime contactCreatedAt = contact.getCreatedAt();
        if (contactCreatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = contactCreatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }

        LocalDateTime contactUpdatedAt = contact.getUpdatedAt();
        if (contactUpdatedAt != null) {
            ZonedDateTime utcDateTime = contactUpdatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dto.setUpdatedAt(utcDateTime.format(formatter));
        }

        return dto;
    }

    public void updateEntityFromDTO(ContactRequestDTO dto, Contact contact) {
        if (dto == null || contact == null) {
            return;
        }
        BeanUtils.copyProperties(dto, contact, "id", "status", "createdAt", "updatedAt", "ipAddress", "userAgent", "user");
    }

    public Contact toEntityWithUser(ContactRequestDTO dto, User user) {
        Contact contact = toEntity(dto);
        if (user != null) {
            contact.setUser(user);
        }
        return contact;
    }

    /**
     * Utility method to get null property names for exclusion during copy.
     */
    private String[] getNullPropertyNames(Object source) {
        final java.beans.BeanInfo beanInfo;
        try {
            beanInfo = java.beans.Introspector.getBeanInfo(source.getClass());
        } catch (java.beans.IntrospectionException e) {
            return new String[0];
        }

        java.util.List<String> nullProperties = new java.util.ArrayList<>();
        for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                Object srcValue = pd.getReadMethod().invoke(source);
                if (srcValue == null) {
                    nullProperties.add(pd.getName());
                }
            } catch (Exception e) {
                // Handle exception or log as needed
            }
        }
        return nullProperties.toArray(new String[0]);
    }
}