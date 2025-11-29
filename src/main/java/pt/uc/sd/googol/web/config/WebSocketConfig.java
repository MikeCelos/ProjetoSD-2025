package pt.uc.sd.googol.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Onde o servidor envia mensagens para o cliente
        config.enableSimpleBroker("/topic");
        // Prefixo para mensagens do cliente para o servidor (se houver)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // O ponto de entrada que o JavaScript do teu colega usa: var socket = new SockJS('/ws-stats');
        registry.addEndpoint("/ws-stats").setAllowedOriginPatterns("*").withSockJS();
    }
}