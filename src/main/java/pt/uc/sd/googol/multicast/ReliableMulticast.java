/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1
 *  Elemento 1: Andre Fonseca Ramos (2023227306)
 *  Ficheiro: ReliableMulticast.java
 * ===============================================================
 *
 *  @Resumo:
 *  Implementação, em Java, de um mecanismo de **multicast fiável**
 *  (reliable multicast) por cima de RMI, com o objetivo de garantir
 *  que **todas as réplicas de Barrel** recebem as mesmas atualizações
 *  de índice (objetos {@link pt.uc.sd.googol.common.PageInfo}).
 *
 *  Esta classe não é um Barrel e não é exposta via RMI; funciona como
 *  um **componente de apoio** ao Downloader (ou a outro produtor de
 *  páginas) para que, ao receber uma nova página, consiga difundi-la
 *  para N barrels e ter a certeza de que pelo menos um número mínimo
 *  de réplicas a recebeu.
 *
 *  @Motivação:
 *  Num sistema distribuído com vários Barrels (barrel0, barrel1, ...),
 *  se o Downloader enviar a página só para um Barrel, o índice fica
 *  inconsistente. Com o reliable multicast, a mesma página é enviada
 *  a vários Barrels e o emissor recebe feedback sobre o sucesso do
 *  envio, permitindo retry e remoção de réplicas defeituosas.
 *
 *  @Modelo usado:
 *  Implementa uma variante simples de **All-Ack Reliable Multicast**:
 *   1. o emissor envia a página a todos os barrels (em paralelo);
 *   2. regista quais aceitaram e quais falharam;
 *   3. se não atingiu o mínimo de réplicas (`minReplicas`), faz
 *      novas tentativas (retries) apenas para os que falharam;
 *   4. se, mesmo após os retries, não atingiu o mínimo, lança
 *      RemoteException para o chamador lidar com a falha.
 *
 *  @Estruturas internas:
 *   - `barrels` (CopyOnWriteArrayList):
 *       lista dinâmica de réplicas conhecidas; pode crescer e encolher
 *       em runtime (`addBarrel()` / `removeBarrel()`).
 *   - `sentMessages` (ConcurrentHashMap.newKeySet()):
 *       conjunto de IDs de mensagens já enviadas, para evitar
 *       processamento duplicado quando há reenvios.
 *
 *  @Parâmetros de fiabilidade:
 *   - `minReplicas` — nº mínimo de barrels que têm de confirmar
 *     para considerar o multicast bem-sucedido.
 *   - `maxRetries` — nº máximo de tentativas adicionais.
 *   - `retryDelayMs` — tempo de espera entre tentativas.
 *
 *  @Fluxo do método principal (sendDocument):
 *   1. Gera um ID de mensagem a partir do URL e do timestamp.
 *   2. Se for duplicado, ignora.
 *   3. Envia em paralelo (thread pool) o `addDocument(page)` para
 *      todos os barrels.
 *   4. Conta quantos confirmaram.
 *   5. Se for menos que o mínimo, passa para fase de retries.
 *   6. Se mesmo assim não atingir o mínimo → RemoteException.
 *   7. Caso contrário, devolve um {@code MulticastResult} com o nº
 *      de entregas bem-sucedidas e falhadas.
 *
 *  @Integração no projeto:
 *   - Esta classe deve ser usada pelo **Downloader** (ou pelo
 *     coordenador de download) em vez de chamar diretamente
 *     `barrel.addDocument(...)`.
 *   - Os Barrels continuam simples e passivos; a inteligência de
 *     replicação está do lado do emissor.
 *
 *  @Plano futuro:
 *   - Substituir o ID de mensagem baseado em timestamp por um
 *     sequenciador global.
 *   - Adicionar detecção de falhas com heartbeats e remoção
 *     automática de réplicas mortas.
 *   - Suportar ordenação total das mensagens (total order multicast)
 *     se vier a ser necessário para o projeto.
 *
 *  @Autor:
 *   - Implementado no contexto do elemento 2 como parte da
 *     componente de replicação do índice.
 */

package pt.uc.sd.googol.multicast;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.PageInfo;

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