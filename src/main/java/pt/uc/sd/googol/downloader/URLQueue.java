package pt.uc.sd.googol.downloader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class URLQueue {
    private final LinkedBlockingQueue<String> queue;
    private final Set<String> visited; // URLs já visitados
    private final Set<String> queued;  // URLs já na fila
    
    public URLQueue() {
        this.queue = new LinkedBlockingQueue<>();
        this.visited = ConcurrentHashMap.newKeySet();
        this.queued = ConcurrentHashMap.newKeySet();
    }
    
    public synchronized void addURL(String url) {
        // Não adicionar se já foi visitado ou já está na fila
        if (!visited.contains(url) && !queued.contains(url)) {
            queue.offer(url);
            queued.add(url);
            System.out.println("URL adicionado à fila: " + url);
        }
    }
    
    public String getNextURL() throws InterruptedException {
        String url = queue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
        if (url != null) {
            queued.remove(url);
        }
        return url;
    }
    
    public synchronized void markAsVisited(String url) {
        visited.add(url);
    }
    
    public int getQueueSize() {
        return queue.size();
    }
    
    public int getVisitedCount() {
        return visited.size();
    }
}