package pt.uc.sd.googol.queue;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação da Fila de URLs (URL Queue).
 * <p>
 * Esta classe atua como o "cérebro" central do processo de crawling. É responsável por:
 * <ul>
 * <li>Armazenar URLs pendentes para processamento.</li>
 * <li>Garantir que URLs não são visitados repetidamente (deduplication).</li>
 * <li>Gerir prioridades (URLs manuais furam a fila).</li>
 * <li>Monitorizar o número de Downloaders ativos no sistema.</li>
 * </ul>
 * A classe é Thread-Safe para suportar múltiplos Downloaders a pedir/inserir URLs simultaneamente.
 *
 * @author André Ramos 2023227306
 */
public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {

    /** Estrutura de dados principal (Deque) para permitir inserção no fim (normal) e no início (prioridade). */
    private final LinkedBlockingDeque<String> queue;
    
    /** Conjunto de URLs que já foram totalmente processados/visitados. */
    private final Set<String> visited;
    
    /** Conjunto de URLs que estão atualmente na fila (para evitar duplicados pendentes). */
    private final Set<String> queued;
    
    /** Contador atómico para rastrear o número de workers ligados em tempo real. */
    private final AtomicInteger activeDownloaders = new AtomicInteger(0);

    /**
     * Construtor da URL Queue.
     * Inicializa as estruturas de dados concorrentes.
     *
     * @throws RemoteException Se ocorrer erro na exportação do objeto RMI.
     */
    public URLQueue() throws RemoteException {
        super();
        // MUDANÇA 3: Instanciar como LinkedBlockingDeque para suportar prioridades
        this.queue = new LinkedBlockingDeque<>();
        this.visited = ConcurrentHashMap.newKeySet();
        this.queued = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Adiciona um URL ao final da fila (comportamento padrão do Crawler).
     * O URL só é adicionado se não tiver sido visitado nem estiver já na fila.
     *
     * @param url O URL a adicionar.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public synchronized void addURL(String url) throws RemoteException {
        if (url == null) return;
        url = url.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        if (!visited.contains(url) && !queued.contains(url)) {
            queue.offer(url); // Adiciona ao fim (comportamento normal)
            queued.add(url);
            System.out.println(" [Queue] + Adicionado: " + url);
        }
    }
    
    /**
     * Adiciona um URL ao INÍCIO da fila (Prioridade Máxima).
     * Usado quando um cliente insere um URL manualmente ("Indexar URL").
     *
     * @param url O URL a indexar com urgência.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public synchronized void addTopPriorityURL(String url) throws RemoteException {
        if (visited.contains(url)) {
            System.out.println(" [Queue] ! URL já visitado, ignorando prioridade: " + url);
            return;
        }
        
        if (!queued.contains(url)) {
            // AGORA JÁ FUNCIONA: LinkedBlockingDeque tem este método
            queue.offerFirst(url); 
            queued.add(url);
            System.out.println(" [Queue] +++ Adicionado (PRIORIDADE MÁXIMA): " + url);
        } else {
             System.out.println(" [Queue] URL já estava na fila (não movido).");
        }
    }
    
    /**
     * Obtém e remove o próximo URL da fila.
     * Se a fila estiver vazia, aguarda até 2 segundos por um novo item.
     *
     * @return O próximo URL a processar, ou null se a fila estiver vazia após o timeout.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public String getNextURL() throws RemoteException {
        try {
            // Poll tira do início da fila (funciona igual no Deque)
            String url = queue.poll(2, TimeUnit.SECONDS);
            if (url != null) {
                queued.remove(url);
                System.out.println(" [Queue] -> Entregue para processamento: " + url);
            }
            return url;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Marca um URL como visitado para evitar reprocessamento futuro.
     *
     * @param url O URL que acabou de ser processado.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public synchronized void markAsVisited(String url) throws RemoteException {
        if (url != null) {
            url = url.trim();
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            visited.add(url);
        }
    }

    /**
     * Retorna o tamanho atual da fila de espera.
     * @return Número de URLs pendentes.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public int getQueueSize() throws RemoteException {
        return queue.size();
    }

    /**
     * Retorna o número total de URLs já visitados desde o início.
     * @return Contagem de URLs processados.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public int getVisitedCount() throws RemoteException {
        return visited.size();
    }
    
    /**
     * Verifica se o servidor da fila está ativo.
     * @return Mensagem de estado.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public String ping() throws RemoteException {
        return "Queue Server Online - Pendentes: " + queue.size();
    }

    /**
     * Regista a entrada de um novo Downloader no sistema.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public void registerDownloader() throws RemoteException {
        int n = activeDownloaders.incrementAndGet();
        System.out.println(" [Queue] Novo Downloader registado. Total: " + n);
    }

    /**
     * Regista a saída de um Downloader do sistema.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public void unregisterDownloader() throws RemoteException {
        int n = activeDownloaders.decrementAndGet();
        // Evitar números negativos por segurança
        if (n < 0) {
            activeDownloaders.set(0);
            n = 0;
        }
        System.out.println(" [Queue] Downloader saiu. Total: " + n);
    }

    /**
     * Obtém o número atual de Downloaders ativos.
     * @return Número de workers conectados.
     * @throws RemoteException Se ocorrer erro RMI.
     */
    @Override
    public int getActiveDownloaders() throws RemoteException {
        return activeDownloaders.get();
    }
}