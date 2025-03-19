package my.project.qri3a.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String token, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Réinitialisation de votre mot de passe - qri3a.ma");

        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("username", username);
        context.setVariable("resetUrl", "http://localhost:4200" + "/auth/reset-password?token=" + token);

        String htmlContent = templateEngine.process("password-reset-email", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    public void sendVerificationEmail(String to, String code, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Vérification de votre adresse email - qri3a.ma");

        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("username", username);

        String htmlContent = templateEngine.process("email-verification", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}