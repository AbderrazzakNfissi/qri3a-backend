package my.project.qri3a.services;

import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.EmailVerificationException;
import my.project.qri3a.exceptions.TooManyAttemptsException;
import my.project.qri3a.exceptions.VerificationCodeExpiredException;
import my.project.qri3a.exceptions.VerificationCodeInvalidException;

import java.util.UUID;

public interface EmailVerificationService {
    void sendVerificationCode(User user)  throws EmailVerificationException,  TooManyAttemptsException, VerificationCodeExpiredException, VerificationCodeInvalidException;
    boolean verifyCode(String code, UUID userId) throws VerificationCodeInvalidException, VerificationCodeExpiredException,  TooManyAttemptsException;
}
