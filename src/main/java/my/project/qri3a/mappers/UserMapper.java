package my.project.qri3a.mappers;

import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.responses.UserResponseDTO;
import my.project.qri3a.entities.User;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        // Handle any special mappings here if necessary
        return user;
    }


    public UserResponseDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDTO dto = new UserResponseDTO();
        BeanUtils.copyProperties(user, dto);
        // Handle any special mappings here if necessary
        return dto;
    }


    public void updateEntityFromDTO(UserRequestDTO dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        BeanUtils.copyProperties(dto, user, getNullPropertyNames(dto));
        // The third parameter excludes null properties to avoid overwriting existing values with null
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
