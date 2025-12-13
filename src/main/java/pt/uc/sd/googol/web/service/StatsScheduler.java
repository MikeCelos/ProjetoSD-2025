/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 2
 *  Ficheiro: StatsScheduler.java
 * ===============================================================
 *
 *  @Resumo:
 *  Serviço responsável pela recolha periódica de estatísticas do
 *  sistema distribuído GOOGOL e pela sua difusão em tempo real
 *  para o Frontend Web através de WebSockets (STOMP).
 *
 *  Esta classe funciona como um mecanismo de monitorização contínua,
 *  permitindo que os utilizadores visualizem o estado do cluster
 *  (Barrels, Downloaders, fila de URLs, latências, etc.) sem necessidade
 *  de refresh da página.
 *
 *  @Papel na arquitetura:
 *  <ul>
 *    <li>Executa na camada Web (Spring Boot).</li>
 *    <li>Comunica com o Backend distribuído via RMI (Gateway).</li>
 *    <li>Transforma dados textuais em estruturas JSON.</li>
 *    <li>Difunde estatísticas em tempo real via WebSocket.</li>
 *  </ul>
 *
 *  @Fluxo de funcionamento:
 *  <ol>
 *    <li>A cada intervalo fixo, consulta o Gateway RMI.</li>
 *    <li>Obtém estatísticas globais sob a forma de texto.</li>
 *    <li>Extrai valores relevantes recorrendo a expressões regulares.</li>
 *    <li>Constrói um objeto JSON com os dados processados.</li>
 *    <li>Envia os dados para todos os clientes Web subscritos.</li>
 *  </ol>
 *
 *  @Tecnologias utilizadas:
 *  <ul>
 *    <li>Spring Scheduling (@Scheduled).</li>
 *    <li>Spring WebSocket (STOMP).</li>
 *    <li>RMI para comunicação com o Gateway.</li>
 *    <li>Regex para parsing de estatísticas textuais.</li>
 *  </ul>
 *
 *  @Resiliência:
 *  - O serviço tolera falhas temporárias do Gateway.
 *  - A aplicação Web continua a funcionar mesmo que o Gateway esteja offline.
 *  - Exceções de comunicação são capturadas e tratadas silenciosamente.
 *
 *  @Autor:
 *   Elemento 1: André Ramos — 2023227306
 *   Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 */

package pt.uc.sd.googol.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pt.uc.sd.googol.gateway.GatewayInterface;

@Service
@EnableScheduling
public class StatsScheduler {

    /** Template do Spring para envio de mensagens via WebSocket (STOMP). */
    private final SimpMessagingTemplate template;
    
    /** Interface para comunicação com o Backend RMI (Gateway). */
    private final GatewayInterface gateway;

    /**
     * Construtor com injeção de dependências.
     * <p>
     * O {@code required = false} no Autowired permite que a aplicação Web inicie mesmo
     * que o Gateway RMI esteja offline (o bean será null), evitando que o site vá abaixo.
     *
     * @param template O gestor de mensagens WebSocket do Spring.
     * @param gateway A interface remota do Gateway.
     */
    @Autowired(required = false)
    public StatsScheduler(SimpMessagingTemplate template,
                          GatewayInterface gateway) {
        this.template = template;
        this.gateway = gateway;
    }

    /**
     * Tarefa agendada que executa a cada 3000ms (3 segundos).
     * <p>
     * Fluxo de execução:
     * 1. Verifica se o Gateway está ativo.
     * 2. Obtém as estatísticas gerais (String) e valores específicos (Queue/Downloaders).
     * 3. Faz o parsing dos dados textuais para um Mapa (JSON).
     * 4. Envia o objeto para o tópico "/topic/stats", onde o JavaScript do browser está à escuta.
     */
    @Scheduled(fixedRate = 3000)
    public void sendStats() {
        if (gateway == null) return;

        try {
            // 1. Obter texto bruto do Gateway
            String rawStats = gateway.getStats();
            
            // 2. Criar objeto JSON para enviar ao Frontend
            Map<String, Object> stats = new HashMap<>();
            
            // --- PARSING DOS DADOS ---
            stats.put("barrelsActive", extractValue(rawStats, "Barrels ativos: (\\d+)"));
            
            // Obter tamanho da fila
            int currentQueueSize = 0;
            try {
                currentQueueSize = gateway.getQueueSize();
            } catch (Exception e) {
                // Ignora erros momentâneos de conexão
            }
            stats.put("queueSize", currentQueueSize);
            
            // Obter número de downloaders ativos
            int downloaders = 0;
            try {
                downloaders = gateway.getActiveDownloaders();
            } catch (Exception e) {}
            stats.put("downloadersActive", downloaders);

            // Top Queries (Placeholder para futura implementação de parsing da lista)
            List<String> topQueries = new ArrayList<>();
            // Se o getStats do Gateway tiver "Top 10", podes tentar parsear aqui
            stats.put("topQueries", topQueries);

            // Parsing das Latências dos Barrels (Regex)
            List<Map<String, Object>> latencies = new ArrayList<>();
            Matcher m = Pattern.compile("Barrel (\\d+): ([\\d\\.]+) ms").matcher(rawStats);
            while(m.find()) {
                Map<String, Object> l = new HashMap<>();
                l.put("barrelId", m.group(1));
                l.put("avgMs", m.group(2));
                latencies.add(l);
            }
            stats.put("barrelLatencies", latencies);

            // 3. Enviar para o tópico público
            if (template != null) {
                template.convertAndSend("/topic/stats", stats);
            }

        } catch (Exception e) {
            System.err.println("Erro ao enviar stats WS: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para extrair valores numéricos de um texto usando Expressões Regulares.
     *
     * @param text O texto fonte onde procurar.
     * @param regex O padrão Regex contendo um grupo de captura.
     * @return O valor encontrado ou "0" se não houver correspondência.
     */
    private String extractValue(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "0";
    }
}