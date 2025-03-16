package my.project.qri3a.services;

import my.project.qri3a.exceptions.ResourceNotFoundException;

public interface PasswordResetService {
    void requestPasswordReset(String email) throws ResourceNotFoundException;
    void resetPassword(String token, String newPassword);
}