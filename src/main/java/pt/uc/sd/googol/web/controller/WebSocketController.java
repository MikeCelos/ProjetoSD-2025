package pt.uc.sd.googol.web.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import pt.uc.sd.googol.web.service.StatsListenerImpl;

import java.util.Map;

@Controller
public class WebSocketController {

    // Quando o Javascript enviar para "/app/request-stats"
    // Nós respondemos para "/topic/stats"
    @MessageMapping("/request-stats")
    @SendTo("/topic/stats")
    public Map<String, Object> sendInitialStats() {
        System.out.println(" [WS] Novo cliente pediu estatísticas iniciais.");
        
        // Vai buscar à cache que criámos no passo anterior
        return StatsListenerImpl.getLastStats();
    }
}