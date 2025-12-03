package pt.uc.sd.googol.gateway;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.queue.URLQueueInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Ponto de entrada (Gateway) do sistema Googol.
 * <p>
 * Atua como intermediário entre os Clientes (CLI ou Web) e o backend distribuído (Barrels e Queue).
 * Responsabilidades principais:
 * <ul>
 * <li>Receber pedidos de pesquisa e encaminhá-los para um Barrel disponível (Load Balancing).</li>
 * <li>Manter uma cache de resultados para melhorar o desempenho.</li>
 * <li>Gerir a lista de Barrels ativos e tolerar falhas (reconexão automática).</li>
 * <li>Fornecer estatísticas agregadas do sistema.</li>
 * <li>Encaminhar novos URLs para a fila de indexação.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
public class Gateway extends UnicastRemoteObject implements GatewayInterface {
    
    /** Lista de Barrels conhecidos e ativos. */
    private final List<BarrelInterface> barrels;
    
    /** Índice para o algoritmo Round-Robin de balanceamento de carga. */
    private int currentBarrelIndex = 0;
    
    /** Cache de resultados de pesquisa (Termo+Página -> Lista de Resultados). */
    private final Map<String, CachedResult> searchCache;
    
    /** Tempo de vida da cache (5 minutos). */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    /** Referência remota para a fila de URLs (para indexação manual). */
    private final URLQueueInterface urlQueue;
    
    /** Contador de pesquisas para o "Top 10". */
    private final Map<String, Integer> searchCounts;
    
    /** Registo de tempos de resposta por Barrel para cálculo de latência média. */
    private final Map<Integer, List<Long>> responseTimes;
    
    /**
     * Construtor do Gateway.
     * Inicializa as estruturas de dados e estabelece a ligação inicial com os componentes.
     *
     * @param barrels Lista inicial de interfaces RMI para os Barrels.
     * @param urlQueue Interface RMI para a fila de URLs.
     * @throws RemoteException Se ocorrer um erro na exportação RMI.
     */
    protected Gateway(List<BarrelInterface> barrels, URLQueueInterface urlQueue) throws RemoteException {
        super();
        this.barrels = barrels;
        this.searchCache = new ConcurrentHashMap<>();
        this.searchCounts = new ConcurrentHashMap<>();
        this.responseTimes = new ConcurrentHashMap<>();
        this.urlQueue = urlQueue;
        
        // Inicializar lista de tempos para cada barrel
        for (int i = 0; i < barrels.size(); i++) {
            responseTimes.put(i, new CopyOnWriteArrayList<>());
        }
        
        System.out.println(" Gateway inicializado com " + barrels.size() + " barrels");
    }

    /**
     * Envia um URL para ser indexado com prioridade máxima.
     * O URL é colocado no início da fila de processamento.
     *
     * @param url O endereço web a indexar.
     * @return true se o URL foi aceite pela fila, false caso contrário.
     * @throws RemoteException Se houver erro de comunicação com a Queue.
     */
    @Override
    public boolean indexUrl(String url) throws RemoteException {
        System.out.println(" Pedido de indexação recebido: " + url);
        if (urlQueue == null) {
            System.err.println(" Erro: Queue não disponível.");
            return false;
        }
        
        try {
            // Validação básica e normalização
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            
            urlQueue.addTopPriorityURL(url);
            System.out.println(" URL enviado para a Queue com sucesso.");
            return true;
        } catch (RemoteException e) {
            System.err.println(" Erro ao contactar a Queue: " + e.getMessage());
            return false;
        }
    }

    /**
     * Realiza uma pesquisa no sistema.
     * Utiliza cache para respostas rápidas e balanceamento de carga (Round-Robin)
     * para distribuir os pedidos entre os Barrels. Inclui mecanismo de failover.
     *
     * @param terms Lista de termos a pesquisar.
     * @param page Número da página de resultados.
     * @return Lista de resultados encontrados.
     * @throws RemoteException Se todos os Barrels falharem.
     */
    @Override
    public List<SearchResult> search(List<String> terms, int page) throws RemoteException {
        if (terms == null || terms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. Atualizar Estatísticas (Top 10)
        for (String term : terms) {
            String lowerTerm = term.toLowerCase().trim();
            if (!lowerTerm.isEmpty()) {
                searchCounts.merge(lowerTerm, 1, Integer::sum);
            }
        }
        
        // Normalizar termos para pesquisa
        List<String> normalizedTerms = terms.stream()
            .map(String::toLowerCase)
            .map(String::trim)
            .collect(Collectors.toList());
        
        String cacheKey = normalizedTerms.toString() + ":" + page;
        
        // 2. Verificar cache
        CachedResult cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            System.out.println(" Cache hit: " + cacheKey);
            return cached.results;
        }
        
        System.out.println(" Pesquisando: " + normalizedTerms + " (página " + page + ")");
        
        // Escolher barrel (round-robin)
        int barrelIdx = currentBarrelIndex; 
        BarrelInterface barrel = getNextBarrel();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 3. Pesquisar no barrel escolhido
            List<SearchResult> results = barrel.search(normalizedTerms, page);
            
            // 4. Registar tempo de resposta para estatísticas
            long duration = System.currentTimeMillis() - startTime;
            responseTimes.computeIfAbsent(barrelIdx, k -> new CopyOnWriteArrayList<>()).add(duration);
            
            // Guardar em cache
            searchCache.put(cacheKey, new CachedResult(results));
            
            System.out.println(" Encontrados " + results.size() + " resultados em " + duration + "ms (Barrel " + barrelIdx + ")");
            return results;
            
        } catch (RemoteException e) {
            System.err.println(" Erro ao pesquisar no barrel: " + e.getMessage());
            
            // Tentar com outro barrel (Failover)
            removeBarrel(barrel);
            if (!barrels.isEmpty()) {
                return search(terms, page); // Retry recursivo
            }
            throw e;
        }
    }
    
    /**
     * Obtém os backlinks para um determinado URL.
     * Tenta variações do URL (http/https/www) para aumentar a probabilidade de encontrar resultados.
     *
     * @param url O URL alvo.
     * @return Lista de URLs que apontam para o alvo.
     * @throws RemoteException Se ocorrer erro na comunicação.
     */
    @Override
    public List<String> getBacklinks(String url) throws RemoteException {
        System.out.println(" Obtendo backlinks de: " + url);
        
        List<String> variations = new ArrayList<>();
        variations.add(url);
        
        if (!url.startsWith("http")) {
            variations.add("https://" + url);
            variations.add("https://www." + url);
            variations.add("http://" + url);
        }
        if (url.endsWith("/")) {
            variations.add(url.substring(0, url.length() - 1));
        } else {
            variations.add(url + "/");
        }

        // Tentar obter de um barrel (com failover)
        int attempts = 0;
        while (attempts < barrels.size()) {
            BarrelInterface barrel = getNextBarrel();
            try {
                for (String v : variations) {
                    List<String> res = barrel.getBacklinks(v);
                    if (!res.isEmpty()) {
                        System.out.println(" Encontrados backlinks para variação: " + v);
                        return res;
                    }
                }
                return new ArrayList<>(); 
                
            } catch (RemoteException e) {
                System.err.println(" Erro no barrel (backlinks). Tentando outro...");
                removeBarrel(barrel);
                attempts++;
            }
        }
        
        throw new RemoteException("Nenhum barrel disponível para backlinks");
    }
    
    /**
     * Compila e retorna as estatísticas completas do sistema.
     * Inclui estado dos Barrels, tempos de resposta, Top 10 pesquisas e contagens globais.
     * Implementa lógica de reconexão automática para Barrels que tenham reiniciado.
     *
     * @return String formatada com o relatório de estado.
     * @throws RemoteException Se ocorrer erro fatal.
     */
    @Override
    public String getStats() throws RemoteException {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estatísticas do Sistema ===\n");
        stats.append("Barrels ativos: ").append(barrels.size()).append("\n");
        stats.append("Entradas em cache: ").append(searchCache.size()).append("\n\n");
        
        // TOP 10 Pesquisas
        stats.append("--- Top 10 Pesquisas ---\n");
        searchCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Ordem decrescente
            .limit(10)
            .forEach(entry -> stats.append(String.format(" '%s': %d vezes\n", entry.getKey(), entry.getValue())));
            
        stats.append("\n--- Tempos Médios de Resposta ---\n");
        // Tempos médios por Barrel
        for (Map.Entry<Integer, List<Long>> entry : responseTimes.entrySet()) {
            int id = entry.getKey();
            List<Long> times = entry.getValue();
            if (times.isEmpty()) {
                stats.append(" Barrel ").append(id).append(": Sem dados\n");
            } else {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                stats.append(String.format(" Barrel %d: %.2f ms (%d pedidos)\n", id, avg, times.size()));
            }
        }
        
        stats.append("\n--- Status dos Barrels ---\n");
        for (int i = 0; i < barrels.size(); i++) {
            try {
                // Tenta falar com o barrel atual
                String barrelStats = barrels.get(i).getStats();
                stats.append(barrelStats).append("\n");
                
            } catch (RemoteException e) {
                // Se falhar, o Barrel pode ter reiniciado. Vamos tentar reconectar!
                try {
                    System.out.println(" [Gateway] Barrel " + i + " não responde. Tentando reconectar...");
                    
                    Registry registry = LocateRegistry.getRegistry(1099);
                    BarrelInterface newBarrelRef = (BarrelInterface) registry.lookup("barrel" + i);
                    
                    // Atualizar a lista com a nova referência
                    barrels.set(i, newBarrelRef);
                    
                    stats.append(newBarrelRef.getStats()).append(" (Reconectado)\n");
                    System.out.println(" [Gateway] ✓ Reconexão bem-sucedida ao Barrel " + i);
                    
                } catch (Exception ex) {
                    stats.append("Barrel ").append(i).append(": OFFLINE (Incontactável)\n");
                }
            }
        }
        
        return stats.toString();
    }
    
    @Override
    public String ping() throws RemoteException {
        return "Gateway OK - " + barrels.size() + " barrels disponíveis";
    }

    /** Remove entradas expiradas da cache de pesquisa. */
    private void cleanExpiredCache() {
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Seleciona o próximo Barrel disponível usando Round-Robin.
     * @return Interface do Barrel escolhido.
     * @throws RemoteException Se não houver Barrels disponíveis.
     */
    private synchronized BarrelInterface getNextBarrel() throws RemoteException {
        if (barrels.isEmpty()) {
            throw new RemoteException("Nenhum barrel disponível");
        }
        if (currentBarrelIndex >= barrels.size()) {
            currentBarrelIndex = 0;
        }
        
        BarrelInterface barrel = barrels.get(currentBarrelIndex);
        currentBarrelIndex = (currentBarrelIndex + 1) % barrels.size();
        return barrel;
    }
    
    /** Remove um Barrel falhado da lista de ativos. */
    private synchronized void removeBarrel(BarrelInterface barrel) {
        barrels.remove(barrel);
        System.err.println(" Barrel removido. Restantes: " + barrels.size());
    }

    @Override
    public int getQueueSize() throws RemoteException {
        if (urlQueue != null) {
            try {
                return urlQueue.getQueueSize();
            } catch (RemoteException e) {
                System.err.println(" Erro ao ler tamanho da fila: " + e.getMessage());
            }
        }
        return 0;
    }
    
    @Override
    public int getActiveDownloaders() throws RemoteException {
        if (urlQueue != null) {
            return urlQueue.getActiveDownloaders();
        }
        return 0;
    }
    
    /** Classe interna para armazenar resultados em cache com timestamp. */
    private static class CachedResult {
        final List<SearchResult> results;
        final long timestamp;
        
        CachedResult(List<SearchResult> results) {
            this.results = results;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL_MS;
        }
    }
    
    /**
     * Ponto de entrada do Gateway.
     * Configura o RMI, descobre os Barrels e a Queue, e inicia o serviço.
     *
     * @param args Argumentos da linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        try {
            int gatewayPort = 1100;
            int barrelPort = 1099;
            int queuePort = 1098; 
            int numBarrels = 2; 
            
            // 1. Conectar aos Barrels
            List<BarrelInterface> barrels = new ArrayList<>();
            Registry barrelRegistry = LocateRegistry.getRegistry("localhost", barrelPort);
            for (int i = 0; i < numBarrels; i++) {
                try {
                    BarrelInterface barrel = (BarrelInterface) barrelRegistry.lookup("barrel" + i);
                    barrels.add(barrel);
                } catch (Exception e) { /* Log erro */ }
            }
            
            // 2. Conectar à Queue
            URLQueueInterface queue = null;
            try {
                Registry queueRegistry = LocateRegistry.getRegistry("localhost", queuePort);
                queue = (URLQueueInterface) queueRegistry.lookup("queue");
                System.out.println("✓ Conectado à URL Queue");
            } catch (Exception e) {
                System.err.println("⚠ AVISO: Não foi possível conectar à Queue. Indexação manual indisponível.");
            }

            // 3. Iniciar Gateway
            Registry gatewayRegistry = LocateRegistry.createRegistry(gatewayPort);
            Gateway gateway = new Gateway(barrels, queue); 
            gatewayRegistry.rebind("gateway", gateway);
            
            System.out.println(" Gateway rodando na porta " + gatewayPort);
            
            // Manter vivo e limpar cache periodicamente
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        gateway.cleanExpiredCache();
                    } catch (InterruptedException e) { break; }
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}