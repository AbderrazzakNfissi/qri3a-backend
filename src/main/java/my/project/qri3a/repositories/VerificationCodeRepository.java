package my.project.qri3a.repositories;

import my.project.qri3a.entities.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findByCode(String code);
    Optional<VerificationCode> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    Optional<VerificationCode> findByCodeAndUserId(String code, UUID userId);
}