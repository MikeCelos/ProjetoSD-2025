package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.multicast.ReliableMulticast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.*;

public class DownloaderWorker implements Runnable {
    private final int workerId;
    private final URLQueue urlQueue;
    private final RobotsTxtParser robotsParser;
    private final ReliableMulticast multicast; // ← MUDOU: agora usa multicast
    private volatile boolean running = true;
    
    public DownloaderWorker(int workerId, URLQueue urlQueue, 
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
                String url = urlQueue.getNextURL();
                
                if (url == null) {
                    Thread.sleep(1000);
                    continue;
                }
                
                if (!robotsParser.isAllowed(url)) {
                    System.out.println("Worker " + workerId + " - URL bloqueado por robots.txt: " + url);
                    urlQueue.markAsVisited(url);
                    continue;
                }
                
                long crawlDelay = robotsParser.getCrawlDelay(url);
                if (crawlDelay > 0) {
                    Thread.sleep(crawlDelay);
                }
                
                System.out.println("Worker " + workerId + " processando: " + url);
                
                PageInfo pageInfo = downloadAndParse(url);
                
                if (pageInfo != null) {
                    urlQueue.markAsVisited(url);
                    
                    // Adicionar novos links à fila
                    for (String newUrl : pageInfo.getLinks()) {
                        urlQueue.addURL(newUrl);
                    }
                    
                    // ← USAR RELIABLE MULTICAST
                    if (multicast != null) {
                        try {
                            System.out.println("Worker " + workerId + " - Enviando via multicast...");
                            ReliableMulticast.MulticastResult result = multicast.sendDocument(pageInfo);
                            System.out.println("Worker " + workerId + " - ✓ " + result);
                        } catch (Exception e) {
                            System.err.println("Worker " + workerId + " - ❌ Multicast falhou: " + e.getMessage());
                            // Tentar novamente após um tempo
                            Thread.sleep(5000);
                            try {
                                multicast.sendDocument(pageInfo);
                            } catch (Exception e2) {
                                System.err.println("Worker " + workerId + " - ❌ Retry final falhou");
                            }
                        }
                    } else {
                        System.out.println("Worker " + workerId + " - ✓ Processado (sem multicast): " + pageInfo.getTitle());
                    }
                }
                
            } catch (InterruptedException e) {
                System.out.println("Worker " + workerId + " interrompido");
                running = false;
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " erro: " + e.getMessage());
            }
        }
        
        System.out.println("Worker " + workerId + " terminado");
    }
    
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
            System.err.println("Erro ao baixar " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    private Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();
        String[] tokens = text.toLowerCase()
                .replaceAll("[^a-záàâãéèêíïóôõöúçñ\\s]", " ")
                .split("\\s+");
        
        for (String token : tokens) {
            if (token.length() > 2) {
                words.add(token);
            }
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