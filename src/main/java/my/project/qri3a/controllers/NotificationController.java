package my.project.qri3a.controllers;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.NotificationRequestDTO;
import my.project.qri3a.dtos.responses.NotificationResponseDTO;
import my.project.qri3a.dtos.responses.UnreadCountDTO;
import my.project.qri3a.entities.Notification;
import my.project.qri3a.entities.Product;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.NotificationMapper;
import my.project.qri3a.repositories.ProductRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.NotificationService;
import my.project.qri3a.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper; // Inject the mapper

    @GetMapping("/me")
    public Page<NotificationResponseDTO> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Récupérer l'email de l'utilisateur authentifié
        String email = authentication.getName();
        // Récupérer l'utilisateur via le userService
        User seller = userService.getUserByEmail(email);
        // Définir le Pageable (par exemple, tri décroissant par date de création)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Récupérer les notifications paginées associées à cet utilisateur
        Page<Notification> notifications = notificationService.getMyNotifications(seller, pageable);
        // Retourner la page de notifications convertie en DTO
        return notifications.map(notificationMapper::toDTO);
    }

    @GetMapping("/unread-count")
    public UnreadCountDTO getUnreadCount(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        long count = notificationService.getUnreadCount(user);
        return new UnreadCountDTO(count);
    }

    @PutMapping("/mark-all-as-seen")
    public void markAllAsSeen(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        notificationService.markAllAsSeen(user);
    }


}
