package my.project.qri3a.repositories;

import my.project.qri3a.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

}
