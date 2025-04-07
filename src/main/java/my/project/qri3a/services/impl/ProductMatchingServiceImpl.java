package my.project.qri3a.services.impl;
import my.project.qri3a.entities.*;
import my.project.qri3a.repositories.NotificationPreferenceRepository;
import my.project.qri3a.repositories.UserPreferenceRepository;
import my.project.qri3a.services.ProductMatchingService;
import my.project.qri3a.services.NotificationService;

import my.project.qri3a.services.TranslationService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMatchingServiceImpl implements ProductMatchingService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationService notificationService;
    private final TranslationService translationService;
    private final UserPreferenceRepository userPreferenceRepository;

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
                // Récupérer la préférence de langue de l'utilisateur
                String userLang = getUserLanguagePreference(user);

                // Récupérer les préférences de l'utilisateur pour construire le message
                List<NotificationPreference> userPreferences = notificationPreferenceRepository.findByUser(user);
                NotificationPreference preference = userPreferences.isEmpty() ? null : userPreferences.get(0);

                // Obtenir le nom de la catégorie traduit selon la préférence de langue
                String categoryName = getTranslatedCategoryName(product.getCategory(), userLang);

                // Construire le message de notification avec le format demandé et traduit
                String notificationMessage = buildNotificationMessage(product, categoryName, preference, userLang);

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

                log.info("Notification envoyée à l'utilisateur {} pour le produit {} en langue {}",
                        user.getId(), product.getId(), userLang);

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
     * Récupère la préférence de langue de l'utilisateur, ou retourne la valeur par défaut (fr)
     */
    private String getUserLanguagePreference(User user) {
        Optional<UserPreference> langPref = userPreferenceRepository.findByUserIdAndKey(user.getId(), "lang");
        return langPref.map(UserPreference::getValue).orElse("fr"); // français par défaut
    }

    /**
     * Construit le message de notification dans la langue demandée
     */
    private String buildNotificationMessage(Product product, String categoryName,
                                            NotificationPreference preference, String lang) {
        String baseMessage;

        switch (lang) {
            case "en":
                baseMessage = String.format(
                        "A new listing matching your criteria has been published: %s matches your search (%s",
                        product.getTitle(), categoryName);

                // Ajouter les détails de prix si disponibles
                if (preference != null && preference.getMinPrice() != null && preference.getMaxPrice() != null) {
                    baseMessage += String.format(", price between %d MAD and %d MAD",
                            preference.getMinPrice().intValue(),
                            preference.getMaxPrice().intValue());
                }
                break;

            case "arm":
                baseMessage = String.format(
                        "إعلان جديد يتوافق مع معاييرك تم نشره: %s يتطابق مع بحثك (%s",
                        product.getTitle(), categoryName);

                // Ajouter les détails de prix si disponibles
                if (preference != null && preference.getMinPrice() != null && preference.getMaxPrice() != null) {
                    baseMessage += String.format("، السعر بين %d درهم و %d درهم",
                            preference.getMinPrice().intValue(),
                            preference.getMaxPrice().intValue());
                }
                break;

            case "fr":
            default:
                baseMessage = String.format(
                        "Une nouvelle annonce correspondant à vos critères a été publiée: %s correspond à votre recherche (%s",
                        product.getTitle(), categoryName);

                // Ajouter les détails de prix si disponibles
                if (preference != null && preference.getMinPrice() != null && preference.getMaxPrice() != null) {
                    baseMessage += String.format(", prix entre %d MAD et %d MAD",
                            preference.getMinPrice().intValue(),
                            preference.getMaxPrice().intValue());
                }
                break;
        }

        return baseMessage + ")";
    }

    /**
     * Traduit le nom de la catégorie selon la langue demandée
     */
    private String getTranslatedCategoryName(Enum<?> category, String lang) {
        String categoryString = category.toString();

        // Français (par défaut)
        if ("fr".equals(lang)) {
            return getCategoryDisplayNameFr(categoryString);
        }
        // Anglais
        else if ("en".equals(lang)) {
            return getCategoryDisplayNameEn(categoryString);
        }
        // Arabe marocain
        else if ("arm".equals(lang)) {
            return getCategoryDisplayNameArm(categoryString);
        }
        // Autre langue non prise en charge -> français par défaut
        else {
            return getCategoryDisplayNameFr(categoryString);
        }
    }

    /**
     * Nom des catégories en français
     */
    private String getCategoryDisplayNameFr(String categoryString) {
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

    /**
     * Nom des catégories en anglais
     */
    private String getCategoryDisplayNameEn(String categoryString) {
        switch (categoryString) {
            case "SMARTPHONES_AND_TELEPHONES":
                return "Smartphones and Phones";
            case "TABLETS_AND_E_BOOKS":
                return "Tablets and E-books";
            case "LAPTOPS":
                return "Laptops";
            case "DESKTOP_COMPUTERS":
                return "Desktop Computers";
            case "TELEVISIONS":
                return "Televisions";
            case "ELECTRO_MENAGE":
                return "Home Appliances";
            case "ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS":
                return "Accessories for Smartphones and Tablets";
            case "SMARTWATCHES_AND_ACCESSORIES":
                return "Smartwatches and Accessories";
            case "AUDIO_AND_HIFI":
                return "Audio and Hi-Fi";
            case "COMPUTER_COMPONENTS":
                return "Computer Components";
            case "STORAGE_AND_PERIPHERALS":
                return "Storage and Peripherals";
            case "PRINTERS_AND_SCANNERS":
                return "Printers and Scanners";
            case "DRONES_AND_ACCESSORIES":
                return "Drones and Accessories";
            case "NETWORK_EQUIPMENT":
                return "Network Equipment";
            case "SMART_HOME_DEVICES":
                return "Smart Home Devices";
            case "GAMING_ACCESSORIES":
                return "Gaming Accessories";
            case "PHOTO_AND_VIDEO_EQUIPMENT":
                return "Photo and Video Equipment";
            default:
                return categoryString.replace("_", " ");
        }
    }

    /**
     * Nom des catégories en arabe marocain
     */
    private String getCategoryDisplayNameArm(String categoryString) {
        switch (categoryString) {
            case "SMARTPHONES_AND_TELEPHONES":
                return "الهواتف الذكية";
            case "TABLETS_AND_E_BOOKS":
                return "الأجهزة اللوحية والكتب الإلكترونية";
            case "LAPTOPS":
                return "أجهزة الكمبيوتر المحمولة";
            case "DESKTOP_COMPUTERS":
                return "أجهزة الكمبيوتر المكتبية";
            case "TELEVISIONS":
                return "أجهزة التلفزيون";
            case "ELECTRO_MENAGE":
                return "الأجهزة المنزلية";
            case "ACCESSORIES_FOR_SMARTPHONES_AND_TABLETS":
                return "إكسسوارات للهواتف الذكية والأجهزة اللوحية";
            case "SMARTWATCHES_AND_ACCESSORIES":
                return "الساعات الذكية والإكسسوارات";
            case "AUDIO_AND_HIFI":
                return "الصوت والهاي فاي";
            case "COMPUTER_COMPONENTS":
                return "مكونات الكمبيوتر";
            case "STORAGE_AND_PERIPHERALS":
                return "التخزين والأجهزة الطرفية";
            case "PRINTERS_AND_SCANNERS":
                return "الطابعات والماسحات الضوئية";
            case "DRONES_AND_ACCESSORIES":
                return "الطائرات بدون طيار والإكسسوارات";
            case "NETWORK_EQUIPMENT":
                return "معدات الشبكة";
            case "SMART_HOME_DEVICES":
                return "أجهزة المنزل الذكية";
            case "GAMING_ACCESSORIES":
                return "إكسسوارات الألعاب";
            case "PHOTO_AND_VIDEO_EQUIPMENT":
                return "معدات الصور والفيديو";
            default:
                return categoryString.replace("_", " ");
        }
    }
}