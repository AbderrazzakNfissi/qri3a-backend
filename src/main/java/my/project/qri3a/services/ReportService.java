package my.project.qri3a.services;
import java.util.UUID;

import org.springframework.stereotype.Service;

import my.project.qri3a.entities.Report;
import my.project.qri3a.entities.User;


@Service
public interface ReportService {
    Report createReportForUser(User reporter, UUID reportedUserId, String reason);
    Report createReportForReview(User reporter, UUID reviewId, String reason);
}
