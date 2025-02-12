package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.NotificationRequestDTO;
import my.project.qri3a.dtos.responses.NotificationResponseDTO;
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

    // Create a notification
    @PostMapping
    public NotificationResponseDTO createNotification(@RequestBody NotificationRequestDTO requestDTO) throws ResourceNotFoundException {
        // Retrieve the Product and the User by their IDs from the DTO
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Convert the DTO into an entity
        Notification notification = Notification.builder()
                .body(requestDTO.getBody())
                .category(requestDTO.getCategory())
                .product(product)
                .user(user)
                .build();

        Notification savedNotification = notificationService.createNotification(notification);
        // Use the NotificationMapper to convert the entity into a DTO
        return notificationMapper.toDTO(savedNotification);
    }

    // Retrieve a notification by its ID
    @GetMapping("/{id}")
    public NotificationResponseDTO getNotificationById(@PathVariable UUID id) {
        Notification notification = notificationService.getNotificationById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        return notificationMapper.toDTO(notification);
    }

    // Retrieve all notifications
    @GetMapping
    public List<NotificationResponseDTO> getAllNotifications() {
        return notificationService.getAllNotifications()
                .stream()
                .map(notificationMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Update a notification
    @PutMapping("/{id}")
    public NotificationResponseDTO updateNotification(@PathVariable UUID id, @RequestBody NotificationRequestDTO requestDTO) {
        // Retrieve the Product and the User by their IDs from the DTO
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Convert the DTO into an entity
        Notification notification = Notification.builder()
                .body(requestDTO.getBody())
                .category(requestDTO.getCategory())
                .product(product)
                .user(user)
                .build();

        Notification updatedNotification = notificationService.updateNotification(id, notification);
        return notificationMapper.toDTO(updatedNotification);
    }

    // Delete a notification
    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
    }

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
}
