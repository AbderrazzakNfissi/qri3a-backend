package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import java.util.List;
import java.util.UUID;


public interface ReviewService {

    ReviewResponseDTO addReview(UUID userId, ReviewRequestDTO reviewRequestDTO);

    void removeReview(UUID userId, UUID reviewId);

    ReviewResponseDTO updateReview(UUID userId, UUID reviewId, ReviewRequestDTO reviewRequestDTO);

    List<ReviewResponseDTO> getAllReviews(UUID userId);
}
