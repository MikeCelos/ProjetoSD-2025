package pt.uc.sd.googol.downloader;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {
    
    private final LinkedBlockingQueue<String> queue;
    private final Set<String> visited; // URLs já visitados
    private final Set<String> queued;  // URLs já na fila
    
    public URLQueue() throws RemoteException {
        super();
        this.queue = new LinkedBlockingQueue<>();
        this.visited = ConcurrentHashMap.newKeySet();
        this.queued = ConcurrentHashMap.newKeySet();
    }
    
    @Override
    public synchronized void addURL(String url) throws RemoteException {
        // Normalização básica
        if (url == null) return;
        url = url.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        // Não adicionar se já foi visitado ou já está na fila
        if (!visited.contains(url) && !queued.contains(url)) {
            queue.offer(url);
            queued.add(url);
            System.out.println(" [Queue] + Adicionado: " + url);
        }
    }
    
    @Override
    public String getNextURL() throws RemoteException {
        try {
            // Espera até 2 segundos por um URL
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
            // Normalização para garantir match
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
}