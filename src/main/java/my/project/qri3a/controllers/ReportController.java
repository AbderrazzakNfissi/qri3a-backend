package my.project.qri3a.controllers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ReportRequestDTO;
import my.project.qri3a.dtos.responses.ReportResponseDTO;
import my.project.qri3a.entities.Report;
import my.project.qri3a.entities.User;
import my.project.qri3a.mappers.ReportMapper;
import my.project.qri3a.services.ReportService;
import my.project.qri3a.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final ReportMapper reportMapper;

    // Endpoint pour signaler un utilisateur
    @PostMapping("/user/{reportedUserId}")
    public ResponseEntity<ReportResponseDTO> reportUser(
            @PathVariable UUID reportedUserId,
            @Valid @RequestBody ReportRequestDTO reportRequest,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (reporter)
        String reporterEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reporterEmail);

        // Créer le signalement
        Report report = reportService.createReportForUser(currentUser, reportedUserId, reportRequest.getReason());

        ReportResponseDTO response = reportMapper.toDTO(report);
        return ResponseEntity.ok(response);
    }

    // Endpoint pour signaler une review
    @PostMapping("/review/{reportedReviewId}")
    public ResponseEntity<ReportResponseDTO> reportReview(
            @PathVariable UUID reportedReviewId,
            @Valid @RequestBody ReportRequestDTO reportRequest,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (reporter)
        String reporterEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reporterEmail);

        // Créer le signalement
        Report report = reportService.createReportForReview(currentUser, reportedReviewId, reportRequest.getReason());
        ReportResponseDTO response = reportMapper.toDTO(report);
        return ResponseEntity.ok(response);
    }
}
