package pt.uc.sd.googol.multicast;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.PageInfo;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementação de um protocolo de Multicast Fiável sobre RMI.
 * <p>
 * Esta classe é responsável por enviar as páginas processadas pelos Downloaders para
 * todos os Barrels ativos no sistema. Garante a consistência e disponibilidade através de:
 * <ul>
 * <li><b>Descoberta Dinâmica:</b> Procura ativamente novos Barrels no Registry se a lista estiver vazia.</li>
 * <li><b>Tolerância a Falhas:</b> Se o envio para um Barrel falhar (ex: reinício do servidor),
 * tenta reconectar automaticamente e reenviar a mensagem.</li>
 * <li><b>Thread Safety:</b> Utiliza estruturas de dados concorrentes para suportar múltiplos workers.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
public class ReliableMulticast {
    
    /** Lista thread-safe de interfaces RMI para os Barrels conhecidos. */
    private final List<BarrelInterface> barrels;
    
    /** Cache de IDs de mensagens enviadas para evitar processamento duplicado local. */
    private final Set<String> sentMessages = ConcurrentHashMap.newKeySet();
    
    /**
     * Construtor do ReliableMulticast.
     * Inicializa a lista de barrels.
     *
     * @param barrels Lista inicial de interfaces RMI para os Barrels.
     */
    public ReliableMulticast(List<BarrelInterface> barrels) {
        this.barrels = new CopyOnWriteArrayList<>(barrels);
        // Se a lista vier vazia, não faz mal, vamos tentar descobrir depois
        System.out.println(" Reliable Multicast inicializado.");
    }
    
    /**
     * Envia um documento (PageInfo) para todos os Barrels registados.
     * <p>
     * Implementa lógica de "best-effort" com recuperação: se um envio falhar,
     * o sistema tenta atualizar a referência RMI desse Barrel e tentar novamente.
     *
     * @param page O objeto PageInfo contendo os dados da página a indexar.
     * @return Um objeto {@link MulticastResult} com o resumo do envio (sucessos/falhas).
     */
    public MulticastResult sendDocument(PageInfo page) {
        String messageId = page.getUrl() + ":" + System.currentTimeMillis();
        if (sentMessages.contains(messageId)) return new MulticastResult(true, 0, 0);
        
        // --- CORREÇÃO: Se não temos barrels, tenta procurar agora ---
        if (barrels.isEmpty()) {
            refreshBarrels();
            if (barrels.isEmpty()) {
                System.err.println(" [Multicast] AVISO: Nenhum barrel encontrado. Dados perdidos: " + page.getUrl());
                return new MulticastResult(false, 0, 0);
            }
        }
        
        int successCount = 0;
        int failCount = 0;
        
        // Tenta enviar para todos
        for (int i = 0; i < barrels.size(); i++) {
            try {
                barrels.get(i).addDocument(page);
                successCount++;
            } catch (RemoteException e) {
                // Se falhar, tenta refrescar a lista e reenviar para o índice específico
                // System.out.println(" [Multicast] Falha no envio. Refrescando ligações...");
                refreshBarrels();
                
                // Tenta uma vez mais no novo objeto (se existir)
                if (i < barrels.size()) {
                    try {
                        barrels.get(i).addDocument(page);
                        successCount++;
                        // System.out.println(" [Multicast] Recuperado!");
                    } catch (Exception ex) {
                        failCount++;
                    }
                } else {
                    failCount++;
                }
            }
        }
        
        if (sentMessages.size() > 1000) sentMessages.clear();
        sentMessages.add(messageId);
        
        return new MulticastResult(successCount > 0, successCount, failCount);
    }
    
    /**
     * Atualiza a lista de Barrels consultando o RMI Registry.
     * Procura por todas as entradas que comecem por "barrel" (ex: barrel0, barrel1)
     * e atualiza a lista interna com as novas referências (stubs).
     */
    private void refreshBarrels() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            String[] list = registry.list();
            
            // Guarda os novos stubs numa lista temporária
            List<BarrelInterface> foundBarrels = new ArrayList<>();
            
            for (String name : list) {
                if (name.startsWith("barrel")) {
                    try {
                        BarrelInterface b = (BarrelInterface) registry.lookup(name);
                        foundBarrels.add(b);
                    } catch (Exception e) { }
                }
            }
            
            // Atualiza a lista principal se encontrou algo
            if (!foundBarrels.isEmpty()) {
                barrels.clear();
                barrels.addAll(foundBarrels);
                // System.out.println(" [Multicast] Lista de Barrels atualizada: " + barrels.size() + " ativos.");
            }
            
        } catch (Exception e) {
            // System.err.println(" [Multicast] Erro ao procurar barrels: " + e.getMessage());
        }
    }
    
    /**
     * Obtém o número de Barrels atualmente conhecidos pelo sistema multicast.
     * @return Número de barrels.
     */
    public int getBarrelCount() { return barrels.size(); }
    
    /**
     * Classe auxiliar (DTO) para reportar o resultado de uma operação de multicast.
     */
    public static class MulticastResult {
        /** Indica se a operação foi considerada globalmente bem sucedida (pelo menos 1 entrega). */
        public final boolean success;
        /** Número de Barrels que confirmaram a receção. */
        public final int delivered;
        /** Número de Barrels que falharam. */
        public final int failed;
        
        public MulticastResult(boolean s, int d, int f) { success=s; delivered=d; failed=f; }
        
        @Override
        public String toString() { return "Multicast: " + delivered + " OK"; }
    }
}