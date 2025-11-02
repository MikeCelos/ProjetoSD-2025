package pt.uc.sd.googol.gateway;

import pt.uc.sd.googol.barrel.BarrelInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {
    
    private final List<BarrelInterface> barrels;
    private int currentBarrelIndex = 0;
    
    // Cache de resultados (termo + página -> resultados)
    private final Map<String, CachedResult> searchCache;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    
    protected Gateway(List<BarrelInterface> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
        this.searchCache = new ConcurrentHashMap<>();
        System.out.println("✓ Gateway inicializado com " + barrels.size() + " barrels");
    }
    
    @Override
    public List<SearchResult> search(List<String> terms, int page) throws RemoteException {
        if (terms == null || terms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Normalizar termos
        List<String> normalizedTerms = terms.stream()
            .map(String::toLowerCase)
            .map(String::trim)
            .collect(Collectors.toList());
        
        String cacheKey = normalizedTerms.toString() + ":" + page;
        
        // Verificar cache
        CachedResult cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            System.out.println("💾 Cache hit: " + cacheKey);
            return cached.results;
        }
        
        System.out.println("🔍 Pesquisando: " + normalizedTerms + " (página " + page + ")");
        
        // Escolher barrel (round-robin)
        BarrelInterface barrel = getNextBarrel();
        
        try {
            // Pesquisar no barrel escolhido
            List<SearchResult> results = barrel.search(normalizedTerms, page);
            
            // Guardar em cache
            searchCache.put(cacheKey, new CachedResult(results));
            
            System.out.println("✓ Encontrados " + results.size() + " resultados");
            return results;
            
        } catch (RemoteException e) {
            System.err.println("❌ Erro ao pesquisar no barrel: " + e.getMessage());
            
            // Tentar com outro barrel
            removeBarrel(barrel);
            if (!barrels.isEmpty()) {
                return search(terms, page); // Retry com outro barrel
            }
            throw e;
        }
    }
    
    @Override
    public List<String> getBacklinks(String url) throws RemoteException {
        System.out.println("🔗 Obtendo backlinks de: " + url);
        
        // Agregar backlinks de todos os barrels
        Set<String> allBacklinks = new HashSet<>();
        
        for (BarrelInterface barrel : barrels) {
            try {
                List<String> backlinks = barrel.getBacklinks(url);
                allBacklinks.addAll(backlinks);
            } catch (RemoteException e) {
                System.err.println("⚠ Erro ao obter backlinks de um barrel: " + e.getMessage());
            }
        }
        
        return new ArrayList<>(allBacklinks);
    }
    
    @Override
    public String getStats() throws RemoteException {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estatísticas do Sistema ===\n");
        stats.append("Barrels ativos: ").append(barrels.size()).append("\n");
        stats.append("Entradas em cache: ").append(searchCache.size()).append("\n\n");
        
        // Estatísticas de cada barrel
        for (int i = 0; i < barrels.size(); i++) {
            try {
                String barrelStats = barrels.get(i).getStats();
                stats.append("Barrel ").append(i).append(":\n");
                stats.append(barrelStats).append("\n");
            } catch (RemoteException e) {
                stats.append("Barrel ").append(i).append(": ERROR\n");
            }
        }
        
        return stats.toString();
    }
    
    @Override
    public String ping() throws RemoteException {
        return "Gateway OK - " + barrels.size() + " barrels disponíveis";
    }

        /**
     * Remove entradas expiradas do cache
     */
    private void cleanExpiredCache() {
        int before = searchCache.size();
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = before - searchCache.size();
        if (removed > 0) {
            System.out.println("🧹 Cache limpo: " + removed + " entradas removidas");
        }
    }
    
    // Load balancing - Round Robin
    private synchronized BarrelInterface getNextBarrel() throws RemoteException {
        if (barrels.isEmpty()) {
            throw new RemoteException("Nenhum barrel disponível");
        }
        
        BarrelInterface barrel = barrels.get(currentBarrelIndex);
        currentBarrelIndex = (currentBarrelIndex + 1) % barrels.size();
        return barrel;
    }
    
    private synchronized void removeBarrel(BarrelInterface barrel) {
        barrels.remove(barrel);
        System.err.println("⚠ Barrel removido. Restantes: " + barrels.size());
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
    
    // ===== MAIN - Iniciar Gateway =====
    public static void main(String[] args) {
        try {
            int gatewayPort = 1100;
            int barrelPort = 1099;
            int numBarrels = 2; // ← Configurar número de barrels
            
            // Conectar aos barrels
            List<BarrelInterface> barrels = new ArrayList<>();
            Registry barrelRegistry = LocateRegistry.getRegistry("localhost", barrelPort);
            
            System.out.println("🔍 Procurando " + numBarrels + " barrels...");
            
            for (int i = 0; i < numBarrels; i++) {
                try {
                    String barrelName = "barrel" + i; // ← SEMPRE barrel0, barrel1, barrel2...
                    BarrelInterface barrel = (BarrelInterface) barrelRegistry.lookup(barrelName);
                    barrel.ping(); // Testar conexão
                    barrels.add(barrel);
                    System.out.println("✓ Conectado ao " + barrelName);
                } catch (Exception e) {
                    System.err.println("⚠ Não foi possível conectar ao barrel" + i + ": " + e.getMessage());
                }
            }
            
            if (barrels.isEmpty()) {
                throw new Exception("Nenhum barrel disponível!");
            }
            
            // Criar registry do gateway
            Registry gatewayRegistry = LocateRegistry.createRegistry(gatewayPort);
            Gateway gateway = new Gateway(barrels);
            gatewayRegistry.rebind("gateway", gateway);
            
            System.out.println("✓ Gateway rodando na porta " + gatewayPort);
            System.out.println("✓ Conectado a " + barrels.size() + " barrel(s)");
            
            // Thread para limpar cache periodicamente
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(60000); // 1 minuto
                        gateway.cleanExpiredCache();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
            
            // Manter vivo
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar Gateway:");
            e.printStackTrace();
        }
    }
}