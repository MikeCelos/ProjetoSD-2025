package pt.uc.sd.googol.web.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import pt.uc.sd.googol.gateway.StatsListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatsListenerImpl extends UnicastRemoteObject implements StatsListener {

    private final SimpMessagingTemplate template;
    
    // CACHE ESTÁTICA
    private static final Map<String, Object> lastKnownStats = new ConcurrentHashMap<>();

    public StatsListenerImpl(SimpMessagingTemplate template) throws RemoteException {
        super();
        this.template = template;
    }

    @Override
    public void onStatsUpdated(Map<String, Object> stats) throws RemoteException {
        if (stats != null) {
            lastKnownStats.putAll(stats);
        }
        template.convertAndSend("/topic/stats", stats);
    }

    public static Map<String, Object> getLastStats() {
        return lastKnownStats;
    }
}