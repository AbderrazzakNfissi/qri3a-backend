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
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour les messages adressés aux méthodes @MessageMapping (si besoin)
        registry.setApplicationDestinationPrefixes("/app");
        // Activation d'un broker simple pour diffuser sur les topics (ici "/topic")
        registry.enableSimpleBroker("/topic");
    }
}
