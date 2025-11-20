package pt.uc.sd.googol.multicast;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.PageInfo;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class ReliableMulticast {
    
    private final List<BarrelInterface> barrels;
    private final Set<String> sentMessages = ConcurrentHashMap.newKeySet();
    
    public ReliableMulticast(List<BarrelInterface> barrels) {
        this.barrels = new CopyOnWriteArrayList<>(barrels);
        // Se a lista vier vazia, não faz mal, vamos tentar descobrir depois
        System.out.println(" Reliable Multicast inicializado.");
    }
    
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
    
    // Método auxiliar para reconectar aos barrels
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
    
    public int getBarrelCount() { return barrels.size(); }
    
    public static class MulticastResult {
        public final boolean success;
        public final int delivered;
        public final int failed;
        public MulticastResult(boolean s, int d, int f) { success=s; delivered=d; failed=f; }
        public String toString() { return "Multicast: " + delivered + " OK"; }
    }
}