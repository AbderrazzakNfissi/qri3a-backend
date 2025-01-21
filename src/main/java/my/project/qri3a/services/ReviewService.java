package my.project.qri3a.services;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.requests.UpdateReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;


public interface ReviewService {

    ReviewResponseDTO addReview(Authentication authentication, ReviewRequestDTO reviewRequestDTO) throws ResourceNotFoundException;

    void removeReview(Authentication authentication, UUID reviewId) throws ResourceNotFoundException, UnauthorizedException;

    ReviewResponseDTO updateReview(Authentication authentication, UUID reviewId, UpdateReviewRequestDTO reviewRequestDTO) throws ResourceNotFoundException;

    List<ReviewResponseDTO> getAllReviews(UUID userId) throws ResourceNotFoundException;
}
