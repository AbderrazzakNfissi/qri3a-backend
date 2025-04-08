package my.project.qri3a.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // L'endpoint auquel le client se connectera (avec SockJS pour la compatibilité)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour les messages adressés aux méthodes @MessageMapping (si besoin)
        registry.setApplicationDestinationPrefixes("/app");
        // Activation d'un broker simple pour diffuser sur les topics généraux et personnels
        registry.enableSimpleBroker("/topic", "/user");
        // Préfixe pour les topics spécifiques aux utilisateurs
        registry.setUserDestinationPrefix("/user");
    }
}