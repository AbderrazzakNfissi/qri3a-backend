package my.project.qri3a.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.requests.UpdateReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.UnauthorizedException;
import my.project.qri3a.services.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    /**
     * Add a new review for a user.
     * @param reviewRequestDTO The review data.
     * @return The created review as ReviewResponseDTO.
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> addReview(
            Authentication authentication,
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO) throws ResourceNotFoundException {
        ReviewResponseDTO createdReview = reviewService.addReview(authentication, reviewRequestDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Delete a review by its ID for a specific user.
     * @param reviewId  The ID of the review to delete.
     * @return No content.
     */
    @DeleteMapping("{reviewId}")
    public ResponseEntity<Void> deleteReview(
            Authentication authentication,
            @PathVariable UUID reviewId) throws ResourceNotFoundException , UnauthorizedException {
        reviewService.removeReview(authentication, reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update an existing review for a user.
     * @param reviewId         The ID of the review to update.
     * @param reviewRequestDTO The updated review data.
     * @return The updated review as ReviewResponseDTO.
     */
    @PutMapping("{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequestDTO reviewRequestDTO) {
        ReviewResponseDTO updatedReview = reviewService.updateReview(authentication, reviewId, reviewRequestDTO);
        return new ResponseEntity<>(updatedReview, HttpStatus.OK);
    }

    /**
     * Get all reviews for a specific user.
     * @param userId The ID of the user.
     * @return A list of reviews as ReviewResponseDTO.
     */
    @GetMapping("{userId}")
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews(
            @PathVariable UUID userId) throws ResourceNotFoundException {
        List<ReviewResponseDTO> reviews = reviewService.getAllReviews(userId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
}
