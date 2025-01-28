package my.project.qri3a.services.impl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.requests.UpdateReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.dtos.responses.ReviewStatisticsResponseDTO;
import my.project.qri3a.entities.Review;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.BadRequestException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.UnauthorizedException;
import my.project.qri3a.mappers.ReviewMapper;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.projections.ReviewStatisticsProjection;
import my.project.qri3a.repositories.ReviewRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ReviewService;
import my.project.qri3a.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Adds a new review authored by the authenticated user for a specified user.
     *
     * @param authentication   The authentication object containing the authenticated user's details.
     * @param reviewRequestDTO The DTO containing review data.
     * @return The created review as ReviewResponseDTO.
     * @throws ResourceNotFoundException If the user being reviewed does not exist.
     */
    @Override
    public ReviewResponseDTO addReview(Authentication authentication, ReviewRequestDTO reviewRequestDTO) throws ResourceNotFoundException, BadRequestException {
        UUID reviewedUserId = reviewRequestDTO.getUserId();
        log.debug("Adding review for userId: {}", reviewedUserId);

        // Récupérer l'utilisateur qui est évalué
        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to be reviewed not found"));

        // Récupérer l'utilisateur authentifié (reviewer) basé sur les détails d'authentification
        String reviewerEmail = authentication.getName();
        User reviewer = userService.getUserByEmail(reviewerEmail);

        // Vérifier que l'utilisateur ne s'évalue pas lui-même
        if (reviewer.getId().equals(reviewedUser.getId())) {
            log.warn("User {} attempted to review themselves.", reviewer.getId());
            throw new BadRequestException("Vous ne pouvez pas vous évaluer vous-même.");
            // Vous pouvez également créer une exception personnalisée si nécessaire
            // throw new BadRequestException("Vous ne pouvez pas vous évaluer vous-même.");
        }

        // Mapper le DTO en entité Review
        Review review = reviewMapper.toEntity(reviewRequestDTO);
        review.setUser(reviewedUser);
        review.setReviewer(reviewer);

        // Gérer éventuellement les relations bidirectionnelles si applicable
        reviewedUser.addReview(review);

        // Sauvegarder l'évaluation dans le repository
        Review savedReview = reviewRepository.save(review);

        log.debug("Review added with id: {}", savedReview.getId());

        return reviewMapper.toDTO(savedReview);
    }


    /**
     * Removes a review authored by the authenticated user.
     *
     * @param authentication The authentication object containing the authenticated user's details.
     * @param reviewId       The ID of the review to remove.
     * @throws ResourceNotFoundException If the review does not exist.
     * @throws UnauthorizedException     If the authenticated user is not the author of the review.
     */
    @Override
    public void removeReview(Authentication authentication, UUID reviewId) throws ResourceNotFoundException, UnauthorizedException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Retrieve the authenticated user (reviewer) based on authentication details
        String reviewerEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reviewerEmail);

        // Check if the current user is the author of the review
        if (!review.getReviewer().getId().equals(currentUser.getId())) {
            log.warn("User {} is not authorized to delete review {}", currentUser.getId(), reviewId);
            throw new UnauthorizedException("You are not authorized to delete this review.");
        }

        // Delete the review from the repository
        reviewRepository.delete(review);

        log.debug("Review with id: {} removed successfully by user: {}", reviewId, currentUser.getId());
    }


    @Override
    public ReviewResponseDTO updateReview(Authentication authentication, UUID reviewId, UpdateReviewRequestDTO reviewRequestDTO) throws ResourceNotFoundException {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Retrieve the authenticated user (reviewer) based on authentication details
        String reviewerEmail = authentication.getName();
        User currentUser = userService.getUserByEmail(reviewerEmail);

        // Check if the current user is the author of the review
        if (!review.getReviewer().getId().equals(currentUser.getId())) {
            log.warn("User {} is not authorized to update review {}", currentUser.getId(), reviewId);
            throw new UnauthorizedException("You are not authorized to update this review.");
        }
        // Use the mapper to update the entity from DTO
        reviewMapper.updateEntityFromDTO(reviewRequestDTO, review);

        Review updatedReview = reviewRepository.save(review);

        log.debug("Review with id: {} updated successfully", reviewId);

        return reviewMapper.toDTO(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getAllReviews(UUID userId) throws ResourceNotFoundException {
        log.debug("Fetching all reviews for userId: {}", userId);

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Review> reviews = reviewRepository.findByUserId(userId);

        List<ReviewResponseDTO> reviewDTOs = reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());

        log.debug("Fetched {} reviews for userId: {}", reviewDTOs.size(), userId);

        return reviewDTOs;
    }

    /**
     * Retrieves review statistics for a specific user using an optimized single query.
     *
     * @param userId The ID of the user.
     * @return The review statistics as ReviewStatisticsResponseDTO.
     * @throws ResourceNotFoundException If the user does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    public ReviewStatisticsResponseDTO getReviewStatistics(UUID userId) throws ResourceNotFoundException {
        log.debug("Fetching review statistics for userId: {}", userId);

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Fetch statistics using the optimized single query
        ReviewStatisticsProjection projection = reviewRepository.getReviewStatisticsByUserId(userId);

        // Handle case where user has no reviews
        if (projection == null) {
            projection = new ReviewStatisticsProjection() {
                @Override
                public Long getOneStarCount() { return 0L; }
                @Override
                public Long getTwoStarCount() { return 0L; }
                @Override
                public Long getThreeStarCount() { return 0L; }
                @Override
                public Long getFourStarCount() { return 0L; }
                @Override
                public Long getFiveStarCount() { return 0L; }
                @Override
                public Double getAverageRating() { return 0.0; }
            };
        }

        // Round the average rating to two decimal places
        Double average = (projection.getAverageRating() != null)
                ? Math.round(projection.getAverageRating() * 100.0) / 100.0
                : 0.0;

        // Construct the response DTO
        ReviewStatisticsResponseDTO statistics = new ReviewStatisticsResponseDTO(
                (projection.getOneStarCount() != null) ? projection.getOneStarCount() : 0L,
                (projection.getTwoStarCount() != null) ? projection.getTwoStarCount() : 0L,
                (projection.getThreeStarCount() != null) ? projection.getThreeStarCount() : 0L,
                (projection.getFourStarCount() != null) ? projection.getFourStarCount() : 0L,
                (projection.getFiveStarCount() != null) ? projection.getFiveStarCount() : 0L,
                average,
                userMapper.toReviewerResponseDTO(user)
        );

        log.debug("Review statistics for userId {}: {}", userId, statistics);

        return statistics;
    }
}
