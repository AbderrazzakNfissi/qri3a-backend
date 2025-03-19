package my.project.qri3a.dtos.requests;

import lombok.Data;

import java.util.UUID;

@Data
public class VerificationRequest {
    private String code;
    private UUID userId;
}

