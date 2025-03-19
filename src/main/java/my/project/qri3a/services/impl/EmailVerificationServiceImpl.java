package my.project.qri3a.services.impl;

import my.project.qri3a.entities.VerificationCode;
import my.project.qri3a.exceptions.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.User;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.repositories.VerificationCodeRepository;
import my.project.qri3a.services.EmailService;
import my.project.qri3a.services.EmailVerificationService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final CacheManager cacheManager;

    @Value("${app.verification-code.expiration-minutes:15}")
    private int expirationMinutes = 15;

    @Value("${app.verification.max-attempts:5}")
    private int maxVerificationAttempts;

    /**
     * Génère un code de vérification et envoie un email
     */
    @Override
    @Transactional
    public void sendVerificationCode(User user) throws TooManyAttemptsException {

        // Vérifier si l'utilisateur est bloqué pour trop de tentatives
        String cacheKey = "verification_attempts:" + user.getId();
        Cache attemptsCache = cacheManager.getCache("verificationAttempts");
        assert attemptsCache != null;
        Integer attempts = attemptsCache.get(cacheKey, Integer.class);

        if (attempts != null && attempts >= maxVerificationAttempts) {
            log.warn("Utilisateur bloqué pour trop de tentatives. UserId: {}", user.getId());
            throw new TooManyAttemptsException("Trop de tentatives. Veuillez réessayer plus tard.");
        }
        // Supprimer les anciens codes
        verificationCodeRepository.deleteByUserId(user.getId());

        // Générer un code à 6 chiffres
        String code = generateRandomCode();

        // Créer et sauvegarder le code de vérification
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setUser(user);
        verificationCode.setExpiryDate(LocalDateTime.now().plusMinutes(expirationMinutes));

        verificationCodeRepository.save(verificationCode);

        // Envoyer l'email
        try {
            emailService.sendVerificationEmail(user.getEmail(), code, user.getName());
            log.info("Code de vérification envoyé à l'utilisateur ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de vérification à l'utilisateur ID: {}", user.getId(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email de vérification: " + e.getMessage());
        }
    }

    /**
     * Vérifie le code et active le compte utilisateur
     */
    @Override
    @Transactional
    public boolean verifyCode(String code, UUID userId) throws TooManyAttemptsException {
        // Vérifier le nombre de tentatives
        String cacheKey = "verification_attempts:" + userId;
        Cache attemptsCache = cacheManager.getCache("verificationAttempts");
        assert attemptsCache != null;
        Integer attempts = attemptsCache.get(cacheKey, Integer.class);

        if (attempts != null && attempts >= maxVerificationAttempts) {
            log.warn("Trop de tentatives de vérification pour l'utilisateur ID: {}", userId);
            throw new TooManyAttemptsException("Trop de tentatives. Veuillez réessayer plus tard ou demandez un nouveau code.");
        }

        try {
            VerificationCode verificationCode = verificationCodeRepository.findByCodeAndUserId(code, userId)
                    .orElseThrow(() -> {
                        // Incrémenter le compteur de tentatives
                        incrementAttempts(attemptsCache, cacheKey, attempts);
                        log.warn("Tentative de vérification avec un code invalide. Code: {}, UserId: {}", code, userId);
                        return new IllegalArgumentException("Code de vérification invalide");
                    });

            if (verificationCode.isExpired()) {
                // Incrémenter le compteur de tentatives
                incrementAttempts(attemptsCache, cacheKey, attempts);
                verificationCodeRepository.delete(verificationCode);
                log.warn("Tentative de vérification avec un code expiré. UserId: {}", userId);
                throw new IllegalArgumentException("Code de vérification expiré");
            }

            User user = verificationCode.getUser();

            if (user.isEmailVerified()) {
                log.info("Utilisateur déjà vérifié. UserId: {}", userId);
                verificationCodeRepository.delete(verificationCode);
                return true;
            }

            user.setEmailVerified(true);
            userRepository.save(user);
            verificationCodeRepository.delete(verificationCode);

            // Réinitialiser le compteur en cas de succès
            attemptsCache.evict(cacheKey);
            log.info("Email vérifié avec succès pour l'utilisateur ID: {}", userId);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * Incrémente le compteur de tentatives
     */
    private void incrementAttempts(Cache attemptsCache, String cacheKey, Integer currentAttempts) {
        int newAttempts = currentAttempts == null ? 1 : currentAttempts + 1;
        attemptsCache.put(cacheKey, newAttempts);
        log.debug("Tentatives de vérification pour {}: {}", cacheKey, newAttempts);

        if (newAttempts >= maxVerificationAttempts) {
            log.warn("Nombre maximal de tentatives atteint pour {}: {}", cacheKey, newAttempts);
        }
    }


    /**
     * Génère un code aléatoire à 6 chiffres
     */
    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Code à 6 chiffres entre 100000 et 999999
        return String.valueOf(code);
    }
}