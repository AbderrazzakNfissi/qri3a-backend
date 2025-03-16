package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.entities.PasswordResetToken;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.repositories.PasswordResetTokenRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.EmailService;
import my.project.qri3a.services.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reset-token.expiration-minutes:15}")
    private int expirationMinutes = 15;


    @Override
    @Transactional
    public void requestPasswordReset(String email) throws ResourceNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun utilisateur trouvé avec l'email: " + email));

        // Vérifier si un token existe déjà pour cet utilisateur
        try {
            // Supprimer les anciens tokens pour cet utilisateur
            tokenRepository.deleteByUser_Id(user.getId());

            // Important: S'assurer que la transaction est bien flushée
            // pour que la suppression soit effective avant d'insérer un nouveau token
            tokenRepository.flush();

            // Créer un nouveau token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(expirationMinutes));

            tokenRepository.save(resetToken);

            // Envoyer l'email
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token, user.getName());
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
            }
        } catch (DataIntegrityViolationException e) {
            // Si on rencontre encore une erreur de contrainte, c'est probablement un problème de timing
            // On peut retenter une fois après une courte attente
            try {
                Thread.sleep(100); // Petite pause
                // Retenter avec une nouvelle transaction
                tokenRepository.deleteByUser_Id(user.getId());
                tokenRepository.flush();

                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(expirationMinutes));

                tokenRepository.save(resetToken);

                emailService.sendPasswordResetEmail(user.getEmail(), token, user.getName());
            } catch (Exception ex) {
                throw new RuntimeException("Impossible de créer un token de réinitialisation: " + ex.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de réinitialisation invalide"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token de réinitialisation expiré");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}