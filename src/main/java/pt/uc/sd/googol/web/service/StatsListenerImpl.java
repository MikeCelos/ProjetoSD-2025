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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pt.uc.sd.googol.gateway.StatsListener;

@Service // ← IMPORTANTE: Registar como Spring Bean
public class StatsListenerImpl extends UnicastRemoteObject implements StatsListener {

    private final SimpMessagingTemplate template;

    // Cache estática (persiste entre chamadas) para guardar o último estado
    private static final Map<String, Object> lastKnownStats = new ConcurrentHashMap<>();

    /**
     * Construtor com injeção de dependências do Spring.
     *
     * @param template Template para enviar mensagens WebSocket
     * @throws RemoteException Se houver erro na exportação RMI
     */
    public StatsListenerImpl(SimpMessagingTemplate template) throws RemoteException {
        super();
        this.template = template;
    }

    /**
     * Método chamado pelo Gateway (via RMI) quando há mudanças nas estatísticas.
     *
     * @param stats Mapa com as estatísticas atualizadas
     * @throws RemoteException Se houver erro na comunicação RMI
     */
    @Override
    public void onStatsUpdated(Map<String, Object> stats) throws RemoteException {
        // Validação: Ignorar se stats for null ou vazio
        if (stats == null || stats.isEmpty()) {
            System.err.println("[LISTENER] ⚠ Recebi stats vazio/null do Gateway!");
            return;
        }

        // Logs detalhados para debug
        System.out.println("╔════════════════════════════════════════════");
        System.out.println("║ [LISTENER] Stats recebidas do Gateway");
        System.out.println("║ Barrels: " + stats.get("barrelsActive"));
        System.out.println("║ Queue: " + stats.get("queueSize"));
        System.out.println("║ Top10 size: " + 
            (stats.get("topQueries") != null ? 
            ((java.util.List)stats.get("topQueries")).size() : "null"));

        // Atualizar cache (limpar antes para não acumular chaves obsoletas)
        lastKnownStats.clear();
        lastKnownStats.putAll(stats);

        System.out.println("║ Cache atualizada. Total chaves: " + lastKnownStats.size());
        System.out.println("║ Enviando para WebSocket (/topic/stats)...");
        System.out.println("╚════════════════════════════════════════════");

        // Enviar para todos os clientes WebSocket conectados
        template.convertAndSend("/topic/stats", stats);
    }

    /**
     * Retorna o último estado conhecido das estatísticas.
     * Usado quando um novo cliente WebSocket se conecta.
     *
     * @return Mapa com as estatísticas ou valores default se ainda não houver dados
     */
    public static Map<String, Object> getLastStats() {
        // Se ainda não recebemos nada do Gateway, retornar valores default
        if (lastKnownStats.isEmpty()) {
            System.out.println("[CACHE] Cache vazia - retornando valores default");
            
            Map<String, Object> defaults = new ConcurrentHashMap<>();
            defaults.put("barrelsActive", 0);
            defaults.put("downloadersActive", 0);
            defaults.put("queueSize", 0);
            defaults.put("topQueries", new ArrayList<>());
            defaults.put("barrelLatencies", new ArrayList<>());
            defaults.put("barrelStorage", new ArrayList<>());
            
            return defaults;
        }

        System.out.println("[CACHE] Retornando cache com " + lastKnownStats.size() + " chaves");
        
        // Retornar cópia para evitar modificações externas
        return new ConcurrentHashMap<>(lastKnownStats);
    }
}