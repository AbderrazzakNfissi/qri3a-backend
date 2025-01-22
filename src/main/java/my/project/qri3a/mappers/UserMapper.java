package my.project.qri3a.mappers;

import my.project.qri3a.dtos.requests.UpdateUserRequestDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.requests.UserSettingsInfosDTO;
import my.project.qri3a.dtos.responses.ReviewerResponseDTO;
import my.project.qri3a.dtos.responses.SellerProfileDTO;
import my.project.qri3a.dtos.responses.UserDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class UserMapper {

    public User toEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        return user;
    }



    public UserResponseDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDTO dto = new UserResponseDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }


    public UserSettingsInfosDTO toUserSettingsInfosDTO(User user) {
        if (user == null) {
            return null;
        }
        UserSettingsInfosDTO dto = new UserSettingsInfosDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    public SellerProfileDTO toSellerProfileDTO(User user) {
        if (user == null) {
            return null;
        }
        SellerProfileDTO dto = new SellerProfileDTO();
        BeanUtils.copyProperties(user, dto);

        LocalDateTime userCreatedAt= user.getCreatedAt();
        if (userCreatedAt != null) {
            // Convertir LocalDateTime en ZonedDateTime avec le fuseau horaire UTC
            ZonedDateTime utcDateTime = userCreatedAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC"));
            // Définir un format ISO 8601
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // Appliquer le format et définir dans le DTO
            dto.setCreatedAt(utcDateTime.format(formatter));
        }
        return dto;
    }
    public ReviewerResponseDTO toReviewerResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        ReviewerResponseDTO dto = new ReviewerResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());

        return dto;
    }


    public void updateEntityFromDTO(UserRequestDTO dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        BeanUtils.copyProperties(dto, user, "id", "createdAt", "updatedAt", "role", "password", "newPassword");
    }

    public void updateEntityFromDTO(UpdateUserRequestDTO dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        BeanUtils.copyProperties(dto, user, "id", "createdAt", "updatedAt","password","newPassword");
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
