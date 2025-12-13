package pt.uc.sd.googol.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pt.uc.sd.googol.web.service.StatsListenerImpl;

import java.util.Map;

/**
 * Controlador WebSocket para comunicação em tempo real.
 * Responsável por enviar estatísticas do sistema para o frontend.
 *
 * @author André Ramos 2023227306
 */
@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Endpoint acionado quando o cliente pede as estatísticas iniciais.
     * Responde com os dados em cache do último estado conhecido.
     *
     * @MessageMapping("/request-stats") - Cliente envia para /app/request-stats
     * Resposta vai para /topic/stats (todos os subscritores recebem)
     */
    @MessageMapping("/request-stats")
    public void sendInitialStats() {
        
        Map<String, Object> lastStats = StatsListenerImpl.getLastStats();
        
        
        // Envia para todos os subscritores de /topic/stats
        messagingTemplate.convertAndSend("/topic/stats", lastStats);
    }
}
