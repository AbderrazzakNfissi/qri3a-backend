package my.project.qri3a.repositories;


import my.project.qri3a.entities.Review;
import my.project.qri3a.projections.ReviewStatisticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByUserId(UUID userId);

    @Query("SELECT " +
            "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) AS oneStarCount, " +
            "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END) AS twoStarCount, " +
            "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END) AS threeStarCount, " +
            "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END) AS fourStarCount, " +
            "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END) AS fiveStarCount, " +
            "AVG(r.rating) AS averageRating " +
            "FROM Review r " +
            "WHERE r.user.id = :userId")
    ReviewStatisticsProjection getReviewStatisticsByUserId(UUID userId);
}
