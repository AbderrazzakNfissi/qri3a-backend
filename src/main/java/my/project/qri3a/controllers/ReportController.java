package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ReportRequestDTO;
import my.project.qri3a.dtos.responses.ReportResponseDTO;
import my.project.qri3a.entities.Report;
import my.project.qri3a.entities.User;
import my.project.qri3a.mappers.ReportMapper;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.ReportService;
import my.project.qri3a.services.UserService;
import org.springframework.http.HttpStatus;
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

    /**
     * Endpoint pour signaler un utilisateur.
     * URL : POST /api/v1/reports/user/{reportedUserId}
     * Corps de la requête : { "reason": "..." }
     */
    @PostMapping("/user/{reportedUserId}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> reportUser(
            @PathVariable UUID reportedUserId,
            @Valid @RequestBody ReportRequestDTO reportRequest,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (reporter)
        String reporterEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reporterEmail);

        // Créer le signalement
        Report report = reportService.createReportForUser(currentUser, reportedUserId, reportRequest.getReason());

        // Mapper l'entité Report en ReportResponseDTO
        ReportResponseDTO responseDTO = reportMapper.toDTO(report);

        // Créer la réponse ApiResponse
        ApiResponse<ReportResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "Signalement de l'utilisateur créé avec succès.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour signaler une review.
     * URL : POST /api/v1/reports/review/{reportedReviewId}
     * Corps de la requête : { "reason": "..." }
     */
    @PostMapping("/review/{reportedReviewId}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> reportReview(
            @PathVariable UUID reportedReviewId,
            @Valid @RequestBody ReportRequestDTO reportRequest,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (reporter)
        String reporterEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reporterEmail);

        // Créer le signalement
        Report report = reportService.createReportForReview(currentUser, reportedReviewId, reportRequest.getReason());

        // Mapper l'entité Report en ReportResponseDTO
        ReportResponseDTO responseDTO = reportMapper.toDTO(report);

        // Créer la réponse ApiResponse
        ApiResponse<ReportResponseDTO> response = new ApiResponse<>(
                responseDTO,
                "Signalement de la review créé avec succès.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }
}
