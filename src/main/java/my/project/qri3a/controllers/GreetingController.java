package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class GreetingController {

    private final SimpMessagingTemplate messagingTemplate;


    // Lorsque le client envoie un message sur "/app/hello", cette méthode est appelée
    @MessageMapping("/hello")
    public void greeting(String message) {
        // Vous pouvez ajouter une logique personnalisée ici si besoin
        // Envoi d'un message "Hello World" au topic "/topic/greetings"
        messagingTemplate.convertAndSend("/topic/greetings", "Hello World");
    }
}
