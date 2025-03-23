package my.project.qri3a.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Utilisateur qui fait le signalement
    @NotNull(message = "Reporter is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private User reporter;

    // Utilisateur signalé (nullable si le signalement concerne une review)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private User reportedUser;

    // Review signalée (nullable si le signalement concerne un utilisateur)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Review reportedReview;

    // Raison du signalement
    @NotBlank(message = "Reason is mandatory")
    @Column(nullable = false, length = 1000)
    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}