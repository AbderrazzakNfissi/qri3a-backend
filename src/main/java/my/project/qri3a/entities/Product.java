package my.project.qri3a.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;
import my.project.qri3a.enums.ProductStatus;
import my.project.qri3a.utils.SlugGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_slug", columnList = "slug")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true, unique = true, length = 255)
    private String slug;

    @Column(nullable = true)
    private String city;

    @Column(nullable = false, length = 60000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String location;

    @Column(nullable = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.MODERATION;

    @Column(nullable = true)
    private String longitude;

    @Column(nullable = true)
    private String latitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private User seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "reportedProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Scam> scamReports = new HashSet<>();

    // Méthodes pour ajouter et retirer des images
    public void addImage(Image image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setProduct(null);
    }

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Livraison
    @Column(nullable = true)
    private String delivery; // "YES" ou "NO"

    @Column(nullable = true)
    private BigDecimal deliveryFee;

    @Column(nullable = true)
    private Boolean deliveryAllMorocco;

    @Column(nullable = true, length = 2000)
    private String deliveryZones; // Stocké comme JSON ou liste séparée par des virgules

    @Column(nullable = true)
    private String deliveryTime; // "1", "3", "7", "14" jours

    public void addScamReport(Scam scam) {
        scamReports.add(scam);
        scam.setReportedProduct(this);
    }

    public void removeScamReport(Scam scam) {
        scamReports.remove(scam);
        scam.setReportedProduct(null);
    }

    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (id != null && title != null) {
            this.slug = SlugGenerator.generateProductSlug(title, id.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}