package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.multicast.ReliableMulticast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.RemoteException; // Necessário para exceções remotas
import java.util.*;

public class DownloaderWorker implements Runnable {
    private final int workerId;
    
    // MUDANÇA 1: Usar a Interface em vez da classe concreta
    private final URLQueueInterface urlQueue; 
    
    private final RobotsTxtParser robotsParser;
    private final ReliableMulticast multicast;
    private volatile boolean running = true;
    
    // MUDANÇA 2: Atualizar o construtor para aceitar a Interface
    public DownloaderWorker(int workerId, URLQueueInterface urlQueue, 
                           RobotsTxtParser robotsParser, ReliableMulticast multicast) {
        this.workerId = workerId;
        this.urlQueue = urlQueue;
        this.robotsParser = robotsParser;
        this.multicast = multicast;
    }
    
    @Override
    public void run() {
        System.out.println("Worker " + workerId + " iniciado");
        
        while (running) {
            try {
                // MUDANÇA 3: getNextURL agora é uma chamada remota (pode lançar RemoteException)
                String url = null;
                try {
                    url = urlQueue.getNextURL();
                } catch (RemoteException e) {
                    System.err.println("Worker " + workerId + " - Erro ao contactar Queue: " + e.getMessage());
                    Thread.sleep(5000); // Espera um pouco se a Queue falhar
                    continue;
                }
                
                if (url == null) {
                    Thread.sleep(1000);
                    continue;
                }
                
                // Verificar Robots.txt
                if (!robotsParser.isAllowed(url)) {
                    System.out.println("Worker " + workerId + " - Bloqueado por robots.txt: " + url);
                    try {
                        urlQueue.markAsVisited(url);
                    } catch (RemoteException e) { /* Ignorar erro de conexão aqui */ }
                    continue;
                }
                
                // Delay (politeness)
                long crawlDelay = robotsParser.getCrawlDelay(url);
                if (crawlDelay > 0) {
                    Thread.sleep(crawlDelay);
                }
                
                System.out.println("Worker " + workerId + " processando: " + url);
                
                // Download da página
                PageInfo pageInfo = downloadAndParse(url);
                
                if (pageInfo != null) {
                    try {
                        urlQueue.markAsVisited(url);
                        
                        // Adicionar novos links à fila remota
                        for (String newUrl : pageInfo.getLinks()) {
                            urlQueue.addURL(newUrl);
                        }
                    } catch (RemoteException e) {
                        System.err.println("Worker " + workerId + " - Erro ao atualizar Queue: " + e.getMessage());
                    }
                    
                    // Enviar para os Barrels (Multicast)
                    if (multicast != null) {
                        try {
                            // System.out.println("Worker " + workerId + " - Enviando via multicast...");
                            ReliableMulticast.MulticastResult result = multicast.sendDocument(pageInfo);
                            // System.out.println("Worker " + workerId + " - " + result);
                        } catch (Exception e) {
                            System.err.println("Worker " + workerId + " - Multicast falhou: " + e.getMessage());
                        }
                    }
                }
                
            } catch (InterruptedException e) {
                running = false;
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " erro genérico: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Worker " + workerId + " terminado");
    }

    // ... (Os métodos privados downloadAndParse, extractWords, extractLinks continuam iguais) ...
    
    private PageInfo downloadAndParse(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(5000)
                    .userAgent("Googol Bot 1.0")
                    .get();
            
            String title = doc.title();
            String text = doc.text();
            Set<String> words = extractWords(text);
            List<String> links = extractLinks(doc);
            String citation = text.length() > 150 
                ? text.substring(0, 150) + "..." 
                : text;
            
            return new PageInfo(url, title, citation, words, links);
            
        } catch (IOException e) {
            // System.err.println("Erro ao baixar " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    private Set<String> extractWords(String text) {
    Set<String> words = new HashSet<>();
    
    String[] tokens = text.toLowerCase().split("[^\\p{L}\\p{N}]+");
    
    for (String token : tokens) {
        // Aceita palavras com 2 ou mais letras (ex: "bi", "ai", "uc")
        if (token.length() >= 2) { 
            words.add(token);
        }
    }
    
    // DEBUG: Ver o que está a ser encontrado na página
    if (!words.isEmpty()) {
    }
    
    return words;
}
    
    private List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements linkElements = doc.select("a[href]");
        
        for (Element link : linkElements) {
            String href = link.attr("abs:href");
            if (href.startsWith("http://") || href.startsWith("https://")) {
                links.add(href);
            }
        }
        return links;
    }

    public void stop() {
        running = false;
    }
}