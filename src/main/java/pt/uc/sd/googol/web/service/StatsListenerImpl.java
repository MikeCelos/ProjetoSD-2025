package pt.uc.sd.googol.web.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import pt.uc.sd.googol.gateway.StatsListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Importante

public class StatsListenerImpl extends UnicastRemoteObject implements StatsListener {

    private final SimpMessagingTemplate template;
    
    // Cache estática para guardar o último estado recebido
    private static final Map<String, Object> lastKnownStats = new ConcurrentHashMap<>();

    public StatsListenerImpl(SimpMessagingTemplate template) throws RemoteException {
        super();
        this.template = template;
    }

    @Override
    public void onStatsUpdated(Map<String, Object> stats) throws RemoteException {
        if (stats != null) {
            lastKnownStats.putAll(stats); // Atualiza cache
        }
        template.convertAndSend("/topic/stats", stats);
    }

    // O método que estava em falta:
    public static Map<String, Object> getLastStats() {
        return lastKnownStats;
    }
}