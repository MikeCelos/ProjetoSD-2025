package pt.uc.sd.googol.downloader;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {

    private final LinkedBlockingDeque<String> queue;
    
    private final Set<String> visited;
    private final Set<String> queued;
    
    private final AtomicInteger activeDownloaders = new AtomicInteger(0);

    public URLQueue() throws RemoteException {
        super();
        // MUDANÇA 3: Instanciar como LinkedBlockingDeque
        this.queue = new LinkedBlockingDeque<>();
        this.visited = ConcurrentHashMap.newKeySet();
        this.queued = ConcurrentHashMap.newKeySet();
    }
    
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
    
    @Override
    public synchronized void markAsVisited(String url) throws RemoteException {
        if (url != null) {
            url = url.trim();
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            visited.add(url);
        }
    }

    @Override
    public int getQueueSize() throws RemoteException {
        return queue.size();
    }

    @Override
    public int getVisitedCount() throws RemoteException {
        return visited.size();
    }
    
    @Override
    public String ping() throws RemoteException {
        return "Queue Server Online - Pendentes: " + queue.size();
    }

    @Override
    public void registerDownloader() throws RemoteException {
        int n = activeDownloaders.incrementAndGet();
        System.out.println(" [Queue] Novo Downloader registado. Total: " + n);
    }

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

    @Override
    public int getActiveDownloaders() throws RemoteException {
        return activeDownloaders.get();
    }
}