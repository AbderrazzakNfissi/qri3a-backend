package my.project.qri3a.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.Role;
import org.hibernate.annotations.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email", unique = true)
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = true)
    private String name;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = true,length = 50)
    private String phoneNumber;

    @Column(nullable = true)
    private String address;

    @Column(nullable = true)
    private String city;

    @NotNull(message = "Role is mandatory")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Min(value = 0, message = "Rating must be at least 0")
    @Max(value = 5, message = "Rating must be at most 5")
    private Float rating;

    @Column(nullable = true,length = 200)
    private String website;

    @Column(nullable = true, length = 500)
    private String aboutMe;

    @Column(nullable = true)
    private String profileImage;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Product> products = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_wishlist",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private List<Product> wishlist = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonManagedReference
    private Set<Review> reviews = new HashSet<>();

    // Signalements faits par cet utilisateur
    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Report> reportsMade = new HashSet<>();

    // Signalements reçus par cet utilisateur
    @OneToMany(mappedBy = "reportedUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Report> reportsReceived = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<NotificationPreference> notificationPreferences = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<UserPreference> preferences = new HashSet<>();


    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    // Après les autres champs, ajoutez :
    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    public void addNotification(Notification notification) {
        notifications.add(notification);
        notification.setUser(this);
    }

    public void removeNotification(Notification notification) {
        notifications.remove(notification);
        notification.setUser(null);
    }

    public void addToWishlist(Product product) {
        wishlist.add(product);
    }

    public void removeFromWishlist(Product product) {
        wishlist.remove(product);
    }

    public void clearWishlist() {
        wishlist.clear();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return role.getAuthorities();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Un utilisateur est actif si son email est vérifié ET s'il n'est pas bloqué
        return emailVerified && !blocked;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void addReview(Review review) {
        reviews.add(review);
        review.setUser(this);
    }

    public void removeReview(Review review) {
        reviews.remove(review);
        review.setUser(null);
    }

    // Méthodes utilitaires
    public void addReportMade(Report report) {
        reportsMade.add(report);
        report.setReporter(this);
    }

    public void removeReportMade(Report report) {
        reportsMade.remove(report);
        report.setReporter(null);
    }

    public void addReportReceived(Report report) {
        reportsReceived.add(report);
        report.setReportedUser(this);
    }

    public void removeReportReceived(Report report) {
        reportsReceived.remove(report);
        report.setReportedUser(null);
    }

    public void addNotificationPreference(NotificationPreference preference) {
        notificationPreferences.add(preference);
        preference.setUser(this);
    }

    public void removeNotificationPreference(NotificationPreference preference) {
        notificationPreferences.remove(preference);
        preference.setUser(null);
    }

    public void addPreference(UserPreference preference) {
        preferences.add(preference);
        preference.setUser(this);
    }

    public void removePreference(UserPreference preference) {
        preferences.remove(preference);
        preference.setUser(null);
    }

    public Optional<UserPreference> getPreferenceByKey(String key) {
        return preferences.stream()
                .filter(p -> p.getKey().equals(key))
                .findFirst();
    }

    public String getPreferenceValue(String key, String defaultValue) {
        return getPreferenceByKey(key)
                .map(UserPreference::getValue)
                .orElse(defaultValue);
    }

    public void setPreference(String key, String value) {
        Optional<UserPreference> existingPref = getPreferenceByKey(key);
        if (existingPref.isPresent()) {
            existingPref.get().setValue(value);
        } else {
            UserPreference newPref = new UserPreference();
            newPref.setKey(key);
            newPref.setValue(value);
            addPreference(newPref);
        }
    }
}
