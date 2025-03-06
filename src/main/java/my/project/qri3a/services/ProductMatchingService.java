package my.project.qri3a.services;

import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.NotificationPreference;
import my.project.qri3a.entities.User;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

@Service
public interface ProductMatchingService {


    /**
     * Trouve tous les utilisateurs dont les préférences correspondent à un produit
     * @param product Le produit à vérifier
     * @return Liste des utilisateurs intéressés par ce produit
     */
    List<User> findInterestedUsers(Product product);

    /**
     * Notifie tous les utilisateurs intéressés par un produit
     * @param product Le produit à notifier
     * @return Nombre de notifications envoyées
     */
    int notifyInterestedUsers(Product product);
}