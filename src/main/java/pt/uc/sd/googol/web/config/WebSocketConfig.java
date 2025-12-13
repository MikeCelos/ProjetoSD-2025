/**
 * Classe de configuração para a comunicação em tempo real via WebSockets.
 * <p>
 * Esta classe ativa e configura o suporte para o protocolo STOMP (Simple Text Oriented Messaging Protocol)
 * sobre WebSockets. É essencial para cumprir o requisito da Meta 2 de "Notificações em tempo real".
 * <p>
 * Define:
 * <ul>
 * <li>O <b>Message Broker</b> em memória para fazer broadcast de mensagens para os clientes.</li>
 * <li>O <b>Endpoint</b> de conexão onde o JavaScript do frontend se liga (via SockJS).</li>
 * </ul>
 *
 * @author Elemento 1: André Ramos (2023227306)
 */

package pt.uc.sd.googol.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura o message broker (agente de mensagens) que encaminha as mensagens
     * de um cliente para outro ou do servidor para os clientes.
     *
     * @param config O registo de configuração do broker.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Ativa um broker simples em memória.
        // O servidor envia mensagens para destinos começados por "/topic" (ex: /topic/stats)
        // e os clientes subscrevem esses tópicos para receber atualizações.
        config.enableSimpleBroker("/topic");
        
        // Define o prefixo para mensagens enviadas do cliente para o servidor.
        // Embora não seja muito usado neste projeto (pois é maioritariamente Server-to-Client),
        // é uma boa prática de configuração padrão.
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Regista os endpoints STOMP, ou seja, os URLs onde o cliente Web se liga
     * para iniciar a sessão WebSocket.
     *
     * @param registry O registo de endpoints.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Cria o endpoint "/ws-stats".
        // .setAllowedOriginPatterns("*"): Permite conexões de qualquer origem (evita erros de CORS).
        // .withSockJS(): Ativa o fallback SockJS, permitindo que funcione mesmo em browsers
        // ou redes que bloqueiem WebSockets puros, degradando para HTTP polling se necessário.
        registry.addEndpoint("/ws-stats").setAllowedOriginPatterns("*").withSockJS();
    }
}