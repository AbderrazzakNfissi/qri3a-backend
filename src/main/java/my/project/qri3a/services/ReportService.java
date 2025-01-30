package my.project.qri3a.services;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.entities.Report;
import my.project.qri3a.entities.Review;
import my.project.qri3a.entities.User;
import my.project.qri3a.repositories.ReportRepository;
import my.project.qri3a.repositories.ReviewRepository;
import my.project.qri3a.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public interface ReportService {
    Report createReportForUser(User reporter, UUID reportedUserId, String reason);
    Report createReportForReview(User reporter, UUID reviewId, String reason);
}
