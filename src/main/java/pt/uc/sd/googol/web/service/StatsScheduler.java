package pt.uc.sd.googol.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pt.uc.sd.googol.gateway.GatewayInterface;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@EnableScheduling
public class StatsScheduler {

    // MUDANÇA: Usar o tipo real em vez de Object
    private final SimpMessagingTemplate template;
    private final GatewayInterface gateway;

    // Construtor com a injeção correta
    @Autowired(required = false)
    public StatsScheduler(SimpMessagingTemplate template,
                          GatewayInterface gateway) {
        this.template = template;
        this.gateway = gateway;
    }

    // Corre a cada 3000ms (3 segundos)
    @Scheduled(fixedRate = 3000)
    public void sendStats() {
        if (gateway == null) return;

        try {
            // 1. Obter texto bruto do Gateway
            String rawStats = gateway.getStats();
            
            // 2. Criar objeto JSON para enviar ao Frontend
            Map<String, Object> stats = new HashMap<>();
            
            // --- PARSING (Mantive a tua lógica) ---
            stats.put("barrelsActive", extractValue(rawStats, "Barrels ativos: (\\d+)"));
            
            int currentQueueSize = 0;
            try {
                currentQueueSize = gateway.getQueueSize();
            } catch (Exception e) {
                // Ignora erros momentâneos
            }
            stats.put("queueSize", currentQueueSize);
            int downloaders = 0;
            try {
                downloaders = gateway.getActiveDownloaders();
            } catch (Exception e) {}
            stats.put("downloadersActive", downloaders);

            // Top Queries
            List<String> topQueries = new ArrayList<>();
            // Se o getStats do Gateway tiver "Top 10", podes tentar parsear aqui
            stats.put("topQueries", topQueries);

            // Latências
            List<Map<String, Object>> latencies = new ArrayList<>();
            Matcher m = Pattern.compile("Barrel (\\d+): ([\\d\\.]+) ms").matcher(rawStats);
            while(m.find()) {
                Map<String, Object> l = new HashMap<>();
                l.put("barrelId", m.group(1));
                l.put("avgMs", m.group(2));
                latencies.add(l);
            }
            stats.put("barrelLatencies", latencies);

            if (template != null) {
                template.convertAndSend("/topic/stats", stats);
            }

        } catch (Exception e) {
            System.err.println("Erro ao enviar stats WS: " + e.getMessage());
        }
    }

    // Helper para sacar números com Regex
    private String extractValue(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "0";
    }
}