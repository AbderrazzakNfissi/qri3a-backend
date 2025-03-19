package my.project.qri3a.services;

import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.TooManyAttemptsException;

import java.util.UUID;

public interface EmailVerificationService {
    void sendVerificationCode(User user) throws TooManyAttemptsException;
    boolean verifyCode(String code, UUID userId) throws TooManyAttemptsException;
}
