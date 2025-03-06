package my.project.qri3a.mappers;


import my.project.qri3a.dtos.requests.NotificationPreferenceDTO;
import my.project.qri3a.dtos.requests.ReportRequestDTO;
import my.project.qri3a.dtos.responses.ReportResponseDTO;
import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.Report;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class NotificationPreferenceMapper {

    public NotificationPreference toEntity(NotificationPreferenceDTO dto) {
        if (dto == null) {
            return null;
        }
        NotificationPreference notificationPreference = new NotificationPreference();
        BeanUtils.copyProperties(dto, notificationPreference,"id");
        return notificationPreference;
    }



    public NotificationPreferenceDTO toDTO(NotificationPreference notificationPreference) {
        if (notificationPreference == null) {
            return null;
        }
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        BeanUtils.copyProperties(notificationPreference, dto);
        return dto;
    }
}
