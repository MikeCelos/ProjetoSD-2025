package pt.uc.sd.googol.web.service;

import pt.uc.sd.googol.web.model.HackerNewsItemRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pt.uc.sd.googol.gateway.GatewayInterface;

import java.rmi.RemoteException;

/**
 * Serviço responsável pela integração com a API REST do Hacker News.
 * <p>
 * Esta classe encarrega-se de:
 * <ul>
 * <li>Consultar a API pública do Hacker News para obter as "top stories".</li>
 * <li>Obter os detalhes de cada história (título, URL, etc.).</li>
 * <li>Filtrar as histórias com base nos termos de pesquisa do utilizador.</li>
 * <li>Enviar os URLs relevantes para o Gateway (RMI) para serem indexados pelo Crawler.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
@Service
public class HackerNewsService {

    /** Interface para comunicação com o Backend RMI (Gateway). */
    private final GatewayInterface gateway;
    
    /** Cliente HTTP do Spring para consumir a API REST externa. */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Construtor com injeção de dependência do Gateway.
     *
     * @param gateway A interface remota do Gateway (configurada em RmiConfig).
     */
    public HackerNewsService(GatewayInterface gateway) {
        this.gateway = gateway;
    }

    /**
     * Processa as principais histórias do Hacker News e envia para indexação as que correspondem à pesquisa.
     * <p>
     * O fluxo de execução é:
     * <ol>
     * <li>Obtém a lista de IDs das "top stories".</li>
     * <li>Itera sobre os primeiros 50 itens.</li>
     * <li>Faz um pedido REST para obter detalhes de cada item.</li>
     * <li>Verifica se o título contém o termo de pesquisa (case-insensitive).</li>
     * <li>Se corresponder, invoca o método {@code indexUrl} do Gateway RMI.</li>
     * </ol>
     *
     * @param query O termo de pesquisa para filtrar as notícias.
     * @return O número de URLs enviados com sucesso para a fila de indexação.
     */
    public int processTopStories(String query) {
        if (gateway == null) {
            System.err.println("Gateway RMI indisponível.");
            return 0;
        }

        System.out.println(" Consultando Hacker News para: " + query);
        
        // 1. Obter lista de IDs das Top Stories
        String idsUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
        Integer[] topIds = restTemplate.getForObject(idsUrl, Integer[].class);

        if (topIds == null) return 0;

        int countIndexed = 0;

        // 2. Buscar detalhes dos primeiros 50 items (limitado para não ser lento)
        for (int i = 0; i < 50 && i < topIds.length; i++) {
            String itemUrl = "https://hacker-news.firebaseio.com/v0/item/" + topIds[i] + ".json";
            
            try {
                HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                // Validar se o item tem os dados necessários
                if (item == null || item.getTitle() == null || item.getUrl() == null) continue;

                // 3. Filtro: Se a pesquisa estiver vazia OU o título contiver o termo
                boolean matches = (query == null || query.isBlank()) || 
                                item.getTitle().toLowerCase().contains(query.toLowerCase());

                if (matches) {
                    System.out.println("   -> Indexando: " + item.getTitle());
                    
                    // 4. Enviar URL para o sistema via RMI
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