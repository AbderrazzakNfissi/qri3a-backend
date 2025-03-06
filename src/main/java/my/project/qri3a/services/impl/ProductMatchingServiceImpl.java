package my.project.qri3a.services.impl;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.User;
import my.project.qri3a.entities.Notification;
import my.project.qri3a.repositories.NotificationPreferenceRepository;
import my.project.qri3a.services.ProductMatchingService;
import my.project.qri3a.services.NotificationService;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMatchingServiceImpl implements ProductMatchingService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationService notificationService;


    @Override
    public List<User> findInterestedUsers(Product product) {
        log.info("Recherche des utilisateurs intéressés par le produit {}", product.getId());

        // Utiliser la nouvelle méthode du repository qui effectue le filtrage côté base de données
        List<User> interestedUsers = notificationPreferenceRepository.findInterestedUsersByProductCriteria(
                product.getCategory(),
                product.getCondition(),
                product.getCity(),
                product.getPrice()
        );

        log.info("Trouvé {} utilisateurs intéressés par le produit {}",
                interestedUsers.size(), product.getId());
        return interestedUsers;
    }

    @Override
    public int notifyInterestedUsers(Product product) {
        log.info("Notification des utilisateurs pour le produit {}", product.getId());

        List<User> interestedUsers = findInterestedUsers(product);
        int notificationCount = 0;

        for (User user : interestedUsers) {
            try {
                // Vérifier si l'utilisateur a activé les notifications par email
                // Cette information pourrait être récupérée avec une requête jointe,
                // mais pour plus de clarté, on la garde séparée ici
                //boolean shouldSendEmail = notificationPreferenceRepository.findByUser(user).stream()
                //         .anyMatch(NotificationPreference::isReceiveEmails);

                // Créer une nouvelle notification
                Notification notification = Notification.builder()
                        .user(user)
                        .product(product)
                        .category(product.getCategory())
                        .body("Une nouvelle annonce correspondant à vos critères a été publiée: " + product.getTitle())
                        .read(false)
                        .build();

                // Enregistrer et envoyer la notification
                notificationService.createNotification(notification);
                notificationCount++;

                log.info("Notification envoyée à l'utilisateur {} pour le produit {}",
                        user.getId(), product.getId());

                // Ici, vous pourriez aussi envoyer un email si shouldSendEmail est true

            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification à l'utilisateur {} pour le produit {}: {}",
                        user.getId(), product.getId(), e.getMessage());
            }
        }

        log.info("{} notifications envoyées avec succès pour le produit {}",
                notificationCount, product.getId());
        return notificationCount;
    }
}