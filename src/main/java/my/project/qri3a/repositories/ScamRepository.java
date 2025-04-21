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

    // Méthodes de recherche par productIdentifier
    Page<Scam> findByProductIdentifier(String productIdentifier, Pageable pageable);

    Page<Scam> findByStatus(ScamStatus status, Pageable pageable);

    @Query("SELECT s FROM Scam s WHERE s.status = :status AND s.productIdentifier = :productIdentifier")
    Page<Scam> findByStatusAndProductIdentifier(
            @Param("status") ScamStatus status,
            @Param("productIdentifier") String productIdentifier,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Scam s WHERE s.status = :status")
    long countByStatus(@Param("status") ScamStatus status);

    @Query("SELECT COUNT(s) FROM Scam s WHERE s.createdAt >= :startDate")
    long countScamsCreatedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT s.productIdentifier) FROM Scam s WHERE s.status = :status")
    long countUniqueProductsWithScamsByStatus(@Param("status") ScamStatus status);

    @Query("SELECT s.productIdentifier FROM Scam s GROUP BY s.productIdentifier HAVING COUNT(s) >= :threshold")
    List<String> findProductsWithScamCountGreaterThan(@Param("threshold") long threshold);

    boolean existsByProductIdentifierAndStatus(
            @Param("productIdentifier") String productIdentifier,
            @Param("status") ScamStatus status);
}