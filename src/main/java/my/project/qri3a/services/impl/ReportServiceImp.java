package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.Report;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.ReportRepository;
import my.project.qri3a.repositories.ReviewRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ReportService;
import org.springframework.stereotype.Service;
import java.util.UUID;
import my.project.qri3a.entities.Review;
import my.project.qri3a.entities.User;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ReportServiceImp implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;


    @Override
    public Report createReportForUser(User reporter, UUID reportedUserId, String reason) throws ResourceNotFoundException {
        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur signalé non trouvé"));

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(reason)
                .build();

        // Ajouter le report aux collections appropriées
        reporter.addReportMade(report);
        reportedUser.addReportReceived(report);

        return reportRepository.save(report);
    }


    @Override
    public Report createReportForReview(User reporter, UUID reviewId, String reason) {
        Review reportedReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review signalée non trouvée"));

        Report report = Report.builder()
                .reporter(reporter)
                .reportedReview(reportedReview)
                .reason(reason)
                .build();

        // Ajouter le report aux collections appropriées
        reporter.addReportMade(report);
        reportedReview.addReport(report);

        return reportRepository.save(report);
    }
}
