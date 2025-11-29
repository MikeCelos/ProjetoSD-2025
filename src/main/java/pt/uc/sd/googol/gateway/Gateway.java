package pt.uc.sd.googol.gateway;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.downloader.URLQueueInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {
    
    private final List<BarrelInterface> barrels;
    private int currentBarrelIndex = 0;
    
    // Cache de resultados (termo + página -> resultados)
    private final Map<String, CachedResult> searchCache;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos

    // Referência para a Queue (para indexar novos URLs)
    private final URLQueueInterface urlQueue;
    
    // Estatísticas
    private final Map<String, Integer> searchCounts; // Top 10 pesquisas
    private final Map<Integer, List<Long>> responseTimes; // Tempos por barrel
    
    // CORREÇÃO AQUI: 'protected' estava escrito 'pprotected'
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

    @Override
    public boolean indexUrl(String url) throws RemoteException {
        System.out.println(" Pedido de indexação recebido: " + url);
        if (urlQueue == null) {
            System.err.println(" Erro: Queue não disponível.");
            return false;
        }
        
        try {
            // Validação básica
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
        int barrelIdx = currentBarrelIndex; // Guardar índice localmente para stats
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
    
    @Override
    public List<String> getBacklinks(String url) throws RemoteException {
        System.out.println(" Obtendo backlinks de: " + url);
        
        // OTIMIZAÇÃO: Consultar apenas 1 barrel (Replicas têm os mesmos dados)
        // TRUQUE: Tentar variações de URL para aumentar probabilidade de match
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
                return new ArrayList<>(); // Se correu variações e não encontrou nada
                
            } catch (RemoteException e) {
                System.err.println(" Erro no barrel (backlinks). Tentando outro...");
                removeBarrel(barrel);
                attempts++;
            }
        }
        
        throw new RemoteException("Nenhum barrel disponível para backlinks");
    }
    
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
                    
                    // 1. Procurar no Registry novamente
                    Registry registry = LocateRegistry.getRegistry(1099); // Porta padrão
                    BarrelInterface newBarrelRef = (BarrelInterface) registry.lookup("barrel" + i);
                    
                    // 2. Atualizar a lista do Gateway com o "novo número de telefone"
                    barrels.set(i, newBarrelRef);
                    
                    // 3. Tentar pedir estatísticas novamente
                    stats.append(newBarrelRef.getStats()).append(" (Reconectado)\n");
                    System.out.println(" [Gateway] ✓ Reconexão bem-sucedida ao Barrel " + i);
                    
                } catch (Exception ex) {
                    // Se mesmo tentando reconectar falhar, então está mesmo morto
                    stats.append("Barrel ").append(i).append(": OFFLINE (Incontactável)\n");
                    // System.err.println(" [Gateway] Falha na reconexão: " + ex.getMessage());
                }
            }
        }
        
        return stats.toString();
    }
    
    @Override
    public String ping() throws RemoteException {
        return "Gateway OK - " + barrels.size() + " barrels disponíveis";
    }

    private void cleanExpiredCache() {
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    // Load balancing - Round Robin
    private synchronized BarrelInterface getNextBarrel() throws RemoteException {
        if (barrels.isEmpty()) {
            throw new RemoteException("Nenhum barrel disponível");
        }
        // Garantir que índice é válido (caso barrels tenham sido removidos)
        if (currentBarrelIndex >= barrels.size()) {
            currentBarrelIndex = 0;
        }
        
        BarrelInterface barrel = barrels.get(currentBarrelIndex);
        currentBarrelIndex = (currentBarrelIndex + 1) % barrels.size();
        return barrel;
    }
    
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
    
    // Classe interna para cache
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
    
    // ===== MAIN =====
    public static void main(String[] args) {
        try {
            int gatewayPort = 1100;
            int barrelPort = 1099;
            int queuePort = 1098; // <--- Porta da Queue
            int numBarrels = 2; 
            
            // 1. Conectar aos Barrels (Igual ao anterior)
            List<BarrelInterface> barrels = new ArrayList<>();
            Registry barrelRegistry = LocateRegistry.getRegistry("localhost", barrelPort);
            for (int i = 0; i < numBarrels; i++) {
                try {
                    BarrelInterface barrel = (BarrelInterface) barrelRegistry.lookup("barrel" + i);
                    barrels.add(barrel);
                } catch (Exception e) { /* Log erro */ }
            }
            
            // 2. Conectar à Queue (NOVO)
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
            // Passamos a queue para o construtor
            Gateway gateway = new Gateway(barrels, queue); 
            gatewayRegistry.rebind("gateway", gateway);
            
            System.out.println(" Gateway rodando na porta " + gatewayPort);
            
            // Manter vivo...
            while(true) Thread.sleep(1000);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}