package my.project.qri3a.services.impl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.requests.ReviewRequestDTO;
import my.project.qri3a.dtos.responses.ReviewResponseDTO;
import my.project.qri3a.entities.Review;
import my.project.qri3a.entities.User;
import my.project.qri3a.mappers.ReviewMapper;
import my.project.qri3a.repositories.ReviewRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.ReviewService;
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

    @Override
    public ReviewResponseDTO addReview(UUID userId, ReviewRequestDTO reviewRequestDTO) {
        log.debug("Adding review for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review = reviewMapper.toEntity(reviewRequestDTO);
        review.setUser(user);

        user.addReview(review);
        Review savedReview = reviewRepository.save(review);

        log.debug("Review added with id: {}", savedReview.getId());

        return reviewMapper.toDTO(savedReview);
    }

    @Override
    public void removeReview(UUID userId, UUID reviewId) {
        log.debug("Removing review with id: {} for userId: {}", reviewId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().equals(user)) {
            throw new IllegalArgumentException("Review does not belong to the specified user");
        }

        user.removeReview(review);
        reviewRepository.delete(review);

        log.debug("Review with id: {} removed successfully", reviewId);
    }

    @Override
    public ReviewResponseDTO updateReview(UUID userId, UUID reviewId, ReviewRequestDTO reviewRequestDTO) {
        log.debug("Updating review with id: {} for userId: {}", reviewId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().equals(user)) {
            throw new IllegalArgumentException("Review does not belong to the specified user");
        }

        // Use the mapper to update the entity from DTO
        reviewMapper.updateEntityFromDTO(reviewRequestDTO, review);

        Review updatedReview = reviewRepository.save(review);

        log.debug("Review with id: {} updated successfully", reviewId);

        return reviewMapper.toDTO(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getAllReviews(UUID userId) {
        log.debug("Fetching all reviews for userId: {}", userId);

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Review> reviews = reviewRepository.findByUserId(userId);

        List<ReviewResponseDTO> reviewDTOs = reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());

        log.debug("Fetched {} reviews for userId: {}", reviewDTOs.size(), userId);

        return reviewDTOs;
    }
}
