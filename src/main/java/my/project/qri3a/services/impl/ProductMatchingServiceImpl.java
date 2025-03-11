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

        // Récupérer le vendeur du produit
        User seller = product.getSeller();

        for (User user : interestedUsers) {
            // Vérifier si l'utilisateur n'est pas le vendeur du produit
            if (seller != null && seller.getId().equals(user.getId())) {
                log.info("Utilisateur {} est le vendeur du produit, notification ignorée", user.getId());
                continue; // Passer à l'utilisateur suivant
            }

            try {
                // Récupérer les préférences de l'utilisateur pour construire le message
                List<NotificationPreference> userPreferences = notificationPreferenceRepository.findByUser(user);
                NotificationPreference preference = userPreferences.isEmpty() ? null : userPreferences.get(0);

                // Construire le message de notification avec le format demandé
                String categoryName = getCategoryDisplayName(product.getCategory());
                String notificationMessage = String.format(
                        "Une nouvelle annonce correspondant à vos critères a été publiée: %s correspond à votre recherche (%s",
                        product.getTitle(), categoryName);

                // Ajouter les détails de prix si disponibles
                if (preference != null && preference.getMinPrice() != null && preference.getMaxPrice() != null) {
                    notificationMessage += String.format(", prix entre %d MAD et %d MAD",
                            preference.getMinPrice().intValue(),
                            preference.getMaxPrice().intValue());
                }

                notificationMessage += ")";

                // Créer une nouvelle notification
                Notification notification = Notification.builder()
                        .user(user)
                        .product(product)
                        .category(product.getCategory())
                        .body(notificationMessage)
                        .read(false)
                        .build();

                // Enregistrer et envoyer la notification
                notificationService.createNotification(notification);
                notificationCount++;

                log.info("Notification envoyée à l'utilisateur {} pour le produit {}",
                        user.getId(), product.getId());

            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification à l'utilisateur {} pour le produit {}: {}",
                        user.getId(), product.getId(), e.getMessage());
            }
        }

        log.info("{} notifications envoyées avec succès pour le produit {}",
                notificationCount, product.getId());
        return notificationCount;
    }
    /**
     * Convertit la catégorie en nom d'affichage plus lisible
     */
    private String getCategoryDisplayName(Enum<?> category) {
        String categoryString = category.toString();

        switch (categoryString) {
            case "SMARTPHONES_AND_TELEPHONES":
                return "Smartphones et Téléphones";
            case "TABLETS_AND_E_BOOKS":
                return "Tablettes et E-books";
            case "LAPTOPS":
                return "Ordinateurs Portables";
            case "DESKTOP_COMPUTERS":
                return "Ordinateurs de Bureau";
            case "TELEVISIONS":
                return "Télévisions";
            case "ELECTRO_MENAGE":
                return "Électroménager";
            case "ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS":
                return "Accessoires pour Smartphones et Tablettes";
            case "SMARTWATCHES_AND_ACCESSORIES":
                return "Smartwatches et Accessoires";
            case "AUDIO_AND_HIFI":
                return "Audio et Hi-Fi";
            case "COMPUTER_COMPONENTS":
                return "Composants Informatiques";
            case "STORAGE_AND_PERIPHERALS":
                return "Stockage et Périphériques";
            case "PRINTERS_AND_SCANNERS":
                return "Imprimantes et Scanners";
            case "DRONES_AND_ACCESSORIES":
                return "Drones et Accessoires";
            case "NETWORK_EQUIPMENT":
                return "Équipement Réseau";
            case "SMART_HOME_DEVICES":
                return "Appareils de Maison Intelligente";
            case "GAMING_ACCESSORIES":
                return "Accessoires de Jeu";
            case "PHOTO_AND_VIDEO_EQUIPMENT":
                return "Équipement Photo et Vidéo";
            default:
                return categoryString.replace("_", " ");
        }
    }
}