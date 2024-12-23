package my.project.qri3a.entities;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.project.qri3a.enums.TokenType;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TokenModel {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(unique = true)
    public String token;

    @Enumerated(EnumType.STRING)
    public TokenType tokenType = TokenType.BEARER;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean revoked;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean expired;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;
}