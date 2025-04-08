package my.project.qri3a.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.entities.Contact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${qri3a.contact.admin-email:admin@qri3a.ma}")
    private String adminEmail;

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

    /**
     * Envoie un email de confirmation à l'utilisateur après soumission d'un formulaire de contact
     *
     * @param contact Les informations de contact soumises
     */
    public void sendContactConfirmationEmail(Contact contact) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(contact.getEmail());
            helper.setSubject("Confirmation de votre message - qri3a.ma");

            Context context = new Context();
            context.setVariable("contact", contact);
            context.setVariable("name", contact.getName());
            context.setVariable("message", contact.getMessage());
            context.setVariable("reason", contact.getReason());
            context.setVariable("contactId", contact.getId().toString());

            String htmlContent = templateEngine.process("contact-confirmation", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Contact confirmation email sent to: {}", contact.getEmail());
        } catch (Exception e) {
            log.error("Failed to send contact confirmation email: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoie une notification par email aux administrateurs lors d'un nouveau contact
     *
     * @param contact Les informations de contact soumises
     */
    public void sendContactNotificationToAdmin(Contact contact) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("Nouveau message de contact - qri3a.ma");

            Context context = new Context();
            context.setVariable("contact", contact);
            context.setVariable("adminUrl", "https://admin.qri3a.ma/contacts/" + contact.getId());

            String htmlContent = templateEngine.process("contact-notification-admin", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Contact notification email sent to admin");
        } catch (Exception e) {
            log.error("Failed to send contact notification email to admin: {}", e.getMessage(), e);
        }
    }

    /**
     * Méthode générique pour envoyer un email
     *
     * @param to Destinataire
     * @param subject Sujet
     * @param content Contenu
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    /**
     * Envoie un email avec pièce jointe
     *
     * @param to Destinataire
     * @param subject Sujet
     * @param content Contenu
     * @param attachmentPath Chemin vers la pièce jointe
     * @param attachmentName Nom de la pièce jointe
     */
    public void sendEmailWithAttachment(String to, String subject, String content,
                                        String attachmentPath, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            org.springframework.core.io.FileSystemResource file =
                    new org.springframework.core.io.FileSystemResource(attachmentPath);
            helper.addAttachment(attachmentName, file);

            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to: {}", to, e);
        }
    }
}