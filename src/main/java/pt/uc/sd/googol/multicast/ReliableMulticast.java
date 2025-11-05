package pt.uc.sd.googol.multicast;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.PageInfo;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementa reliable multicast sobre RMI para garantir que
 * todos os Barrels recebem as mesmas atualizações
 */
public class ReliableMulticast {
    
    private final List<BarrelInterface> barrels;
    private final int minReplicas;
    private final int maxRetries;
    private final long retryDelayMs;
    
    // Tracking de mensagens enviadas para evitar duplicados
    private final Set<String> sentMessages = ConcurrentHashMap.newKeySet();
    
    public ReliableMulticast(List<BarrelInterface> barrels) {
        this(barrels, 2, 3, 1000);
    }
    
    public ReliableMulticast(List<BarrelInterface> barrels, int minReplicas, 
                            int maxRetries, long retryDelayMs) {
        this.barrels = new CopyOnWriteArrayList<>(barrels);
        this.minReplicas = Math.min(minReplicas, barrels.size());
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        
        System.out.println(" Reliable Multicast inicializado:");
        System.out.println("   - Barrels: " + barrels.size());
        System.out.println("   - Min réplicas: " + minReplicas);
        System.out.println("   - Max retries: " + maxRetries);
    }
    
    /**
     * Envia um documento para todos os Barrels com garantia de entrega
     * Implementa: All-Ack Reliable Multicast
     */
    public MulticastResult sendDocument(PageInfo page) throws RemoteException {
        String messageId = generateMessageId(page);
        
        // Evitar duplicados
        if (sentMessages.contains(messageId)) {
            System.out.println(" Mensagem duplicada ignorada: " + messageId);
            return new MulticastResult(true, barrels.size(), 0);
        }
        
        System.out.println(" Multicast: " + page.getUrl());
        
        Map<BarrelInterface, Boolean> deliveryStatus = new ConcurrentHashMap<>();
        
        // Fase 1: Primeiro envio (paralelo para melhor desempenho)
        ExecutorService executor = Executors.newFixedThreadPool(barrels.size());
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (BarrelInterface barrel : barrels) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    barrel.addDocument(page);
                    deliveryStatus.put(barrel, true);
                    return true;
                } catch (RemoteException e) {
                    System.err.println(" Falha no envio para barrel: " + e.getMessage());
                    deliveryStatus.put(barrel, false);
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Aguardar todas as respostas (com timeout)
        for (Future<Boolean> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println(" Timeout ou erro: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        // Contar sucessos
        long successCount = deliveryStatus.values().stream().filter(b -> b).count();
        long failedCount = barrels.size() - successCount;
        
        System.out.println(" Primeira tentativa: " + successCount + "/" + barrels.size() + " OK");
        
        // Fase 2: Retry para os que falharam
        if (successCount < minReplicas && failedCount > 0) {
            System.out.println(" Iniciando retries...");
            
            for (int attempt = 1; attempt <= maxRetries && successCount < minReplicas; attempt++) {
                System.out.println("   Tentativa " + attempt + "/" + maxRetries);
                
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                for (Map.Entry<BarrelInterface, Boolean> entry : deliveryStatus.entrySet()) {
                    if (!entry.getValue()) { // Se falhou antes
                        try {
                            entry.getKey().addDocument(page);
                            entry.setValue(true);
                            successCount++;
                            System.out.println(" Barrel recuperado");
                        } catch (RemoteException e) {
                            System.err.println("  Retry falhou: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // Verificar se atingiu o mínimo
        if (successCount < minReplicas) {
            String error = String.format(
                "Falha no multicast: apenas %d/%d barrels confirmaram (mínimo: %d)",
                successCount, barrels.size(), minReplicas
            );
            System.err.println(" " + error);
            throw new RemoteException(error);
        }
        
        // Marcar como enviada
        sentMessages.add(messageId);
        
        // Limpar cache antigo (manter apenas últimas 10000)
        if (sentMessages.size() > 10000) {
            sentMessages.clear();
        }
        
        System.out.println(" Multicast completo: " + successCount + "/" + barrels.size());
        return new MulticastResult(true, (int) successCount, (int) failedCount);
    }
    
    /**
     * Remove um barrel da lista (quando falha permanentemente)
     */
    public synchronized void removeBarrel(BarrelInterface barrel) {
        if (barrels.remove(barrel)) {
            System.out.println(" Barrel removido. Restantes: " + barrels.size());
        }
    }
    
    /**
     * Adiciona um barrel à lista (quando um novo se junta)
     */
    public synchronized void addBarrel(BarrelInterface barrel) {
        if (!barrels.contains(barrel)) {
            barrels.add(barrel);
            System.out.println(" Barrel adicionado. Total: " + barrels.size());
        }
    }
    
    /**
     * Gera ID único para cada mensagem
     */
    private String generateMessageId(PageInfo page) {
        return page.getUrl() + ":" + System.currentTimeMillis();
    }
    
    public int getBarrelCount() {
        return barrels.size();
    }
    
    public int getMinReplicas() {
        return minReplicas;
    }
    
    /**
     * Classe para retornar resultado do multicast
     */
    public static class MulticastResult {
        public final boolean success;
        public final int delivered;
        public final int failed;
        
        public MulticastResult(boolean success, int delivered, int failed) {
            this.success = success;
            this.delivered = delivered;
            this.failed = failed;
        }
        
        @Override
        public String toString() {
            return String.format("MulticastResult{success=%s, delivered=%d, failed=%d}",
                success, delivered, failed);
        }
    }
}