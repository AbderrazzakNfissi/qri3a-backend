package my.project.qri3a.mappers;
import my.project.qri3a.dtos.requests.ReportRequestDTO;
import my.project.qri3a.dtos.responses.ReportResponseDTO;
import my.project.qri3a.entities.Report;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public Report toEntity(ReportRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Report report = new Report();
        BeanUtils.copyProperties(dto, report);
        return report;
    }



    public ReportResponseDTO toDTO(Report report) {
        if (report == null) {
            return null;
        }
        ReportResponseDTO dto = new ReportResponseDTO();
        BeanUtils.copyProperties(report, dto);
        return dto;
    }
}
