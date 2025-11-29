package pt.uc.sd.googol.web.service;

import pt.uc.sd.googol.web.model.HackerNewsItemRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pt.uc.sd.googol.gateway.GatewayInterface;

import java.rmi.RemoteException;

@Service
public class HackerNewsService {

    private final GatewayInterface gateway;
    private final RestTemplate restTemplate = new RestTemplate();

    // Injeção do Gateway RMI configurado no passo anterior
    public HackerNewsService(GatewayInterface gateway) {
        this.gateway = gateway;
    }

    public int processTopStories(String query) {
        if (gateway == null) {
            System.err.println("Gateway RMI indisponível.");
            return 0;
        }

        System.out.println("🔍 Consultando Hacker News para: " + query);
        
        // 1. Obter lista de IDs das Top Stories
        String idsUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
        Integer[] topIds = restTemplate.getForObject(idsUrl, Integer[].class);

        if (topIds == null) return 0;

        int countIndexed = 0;

        // 2. Buscar detalhes dos primeiros 20 items (limitado para não ser lento)
        for (int i = 0; i < 20 && i < topIds.length; i++) {
            String itemUrl = "https://hacker-news.firebaseio.com/v0/item/" + topIds[i] + ".json";
            
            try {
                HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                // MUDANÇA AQUI: Usa .getTitle() e .getUrl() em vez de .title() e .url()
                if (item == null || item.getTitle() == null || item.getUrl() == null) continue;

                // Filtro
                boolean matches = (query == null || query.isBlank()) || 
                                item.getTitle().toLowerCase().contains(query.toLowerCase());

                if (matches) {
                    System.out.println("   -> Indexando: " + item.getTitle());
                    
                    // MUDANÇA AQUI TAMBÉM:
                    boolean sent = gateway.indexUrl(item.getUrl());
                    
                    if (sent) countIndexed++;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar item HN: " + e.getMessage());
            } 
        }
        
        return countIndexed;
    }
}