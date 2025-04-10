package my.project.qri3a.repositories;
import my.project.qri3a.entities.Scam;
import my.project.qri3a.enums.ScamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScamRepository extends JpaRepository<Scam, UUID> {

    Page<Scam> findByReporterId(UUID reporterId, Pageable pageable);

    Page<Scam> findByReportedProductId(UUID productId, Pageable pageable);

    Page<Scam> findByStatus(ScamStatus status, Pageable pageable);

    @Query("SELECT s FROM Scam s WHERE s.status = :status AND s.reportedProduct.seller.id = :sellerId")
    Page<Scam> findByStatusAndReportedProductSellerId(
            @Param("status") ScamStatus status,
            @Param("sellerId") UUID sellerId,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Scam s WHERE s.status = :status")
    long countByStatus(@Param("status") ScamStatus status);

    @Query("SELECT COUNT(s) FROM Scam s WHERE s.createdAt >= :startDate")
    long countScamsCreatedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT s.reportedProduct.id) FROM Scam s WHERE s.status = :status")
    long countUniqueProductsWithScamsByStatus(@Param("status") ScamStatus status);

    @Query("SELECT s.reportedProduct.id FROM Scam s GROUP BY s.reportedProduct.id HAVING COUNT(s) >= :threshold")
    List<UUID> findProductsWithScamCountGreaterThan(@Param("threshold") long threshold);

    boolean existsByReporterIdAndReportedProductId(UUID reporterId, UUID productId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Scam s " +
            "WHERE s.reportedProduct.id = :productId AND s.status = :status")
    boolean existsByReportedProductIdAndStatus(
            @Param("productId") UUID productId,
            @Param("status") ScamStatus status);
}