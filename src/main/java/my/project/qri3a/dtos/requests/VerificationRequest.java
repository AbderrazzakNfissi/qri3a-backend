package my.project.qri3a.dtos.requests;

import lombok.Data;

@Data
public class VerificationRequest {
    private String code;
    private String email;
}