package my.project.qri3a.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scam_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScamAttachment {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scam_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private Scam scam;

    @NotBlank(message = "Le nom du fichier est obligatoire")
    private String fileName;

    @NotBlank(message = "L'URL du fichier est obligatoire")
    private String fileUrl;

    private String contentType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private AttachmentType type;

    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime uploadedAt;

    public enum AttachmentType {
        SCREENSHOT("screenshots"),
        MESSAGE("messages"),
        PAYMENT_PROOF("payment-proof"),
        PRODUCT_PHOTO("product-photos"),
        LISTING_URL("listing-url"),
        OTHER("other");

        private final String code;

        AttachmentType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static AttachmentType fromCode(String code) {
            for (AttachmentType type : AttachmentType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return OTHER;
        }
    }
}