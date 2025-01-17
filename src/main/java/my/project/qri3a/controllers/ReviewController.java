package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.services.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // Constructor Injection for ReviewService
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Add a new review for a user.
     *
     * @param userId           The ID of the user.
     * @param reviewRequestDTO The review data.
     * @return The created review as ReviewResponseDTO.
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> addReview(
            @PathVariable UUID userId,
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO) {
        ReviewResponseDTO createdReview = reviewService.addReview(userId, reviewRequestDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Delete a review by its ID for a specific user.
     *
     * @param userId    The ID of the user.
     * @param reviewId  The ID of the review to delete.
     * @return No content.
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID userId,
            @PathVariable UUID reviewId) {
        reviewService.removeReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update an existing review for a user.
     *
     * @param userId           The ID of the user.
     * @param reviewId         The ID of the review to update.
     * @param reviewRequestDTO The updated review data.
     * @return The updated review as ReviewResponseDTO.
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable UUID userId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO) {
        ReviewResponseDTO updatedReview = reviewService.updateReview(userId, reviewId, reviewRequestDTO);
        return new ResponseEntity<>(updatedReview, HttpStatus.OK);
    }

    /**
     * Get all reviews for a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of reviews as ReviewResponseDTO.
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews(
            @PathVariable UUID userId) {
        List<ReviewResponseDTO> reviews = reviewService.getAllReviews(userId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
}
