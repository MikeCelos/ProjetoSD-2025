/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 2
 *  Ficheiro: HackerNewsService.java
 * ===============================================================
 *
 *  @Resumo:
 *  Serviço responsável pela integração entre o sistema GOOGOL
 *  e a API REST pública do Hacker News (Y Combinator).
 *
 *  Esta classe permite incorporar conteúdo externo no motor de
 *  pesquisa distribuído, atuando como ponte entre:
 *   - a camada Web (Spring Boot),
 *   - uma API REST externa (Hacker News),
 *   - e o backend distribuído acessível via RMI (Gateway).
 *
 *  @Responsabilidades principais:
 *  <ul>
 *    <li>Consultar a API do Hacker News para obter as "Top Stories".</li>
 *    <li>Obter os detalhes completos de cada item (título, URL, etc.).</li>
 *    <li>Filtrar histórias com base nos termos de pesquisa do utilizador.</li>
 *    <li>Submeter URLs relevantes ao Gateway para indexação.</li>
 *  </ul>
 *
 *  @Papel na arquitetura:
 *  - Executa na camada Web (Spring Boot).
 *  - Consome dados externos via HTTP/REST.
 *  - Comunica com o backend distribuído através de RMI.
 *  - Não executa crawling nem indexação diretamente.
 *
 *  @Tecnologias utilizadas:
 *  - Spring {@link Service} para gestão do ciclo de vida.
 *  - {@link RestTemplate} para consumo de serviços REST externos.
 *  - RMI para comunicação com o Gateway.
 *
 *  @Limitações e decisões de desenho:
 *  - O número de histórias processadas é limitado (50) para evitar
 *    latências excessivas e sobrecarga da API externa.
 *  - O filtro é realizado apenas sobre o título da história.
 *  - Falhas individuais não interrompem o processamento global.
 *
 *  @Autor:
 *   Elemento 1: André Ramos — 2023227306
 */

package pt.uc.sd.googol.web.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.web.model.HackerNewsItemRecord;

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