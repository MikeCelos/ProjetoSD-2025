package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.multicast.ReliableMulticast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import pt.uc.sd.googol.queue.URLQueueInterface;

/**
 * Thread de trabalho (Worker) responsável por descarregar e processar uma página Web.
 * <p>
 * Cada worker opera num ciclo contínuo:
 * <ol>
 * <li>Pede um URL à {@link URLQueueInterface} remota.</li>
 * <li>Verifica se o URL é permitido pelo {@link RobotsTxtParser}.</li>
 * <li>Descarrega o conteúdo HTML usando Jsoup.</li>
 * <li>Extrai título, texto (palavras) e links.</li>
 * <li>Envia os novos links descobertos de volta para a Queue.</li>
 * <li>Envia a informação processada (PageInfo) para os Barrels via Multicast.</li>
 * </ol>
 *
 * @author André Ramos 2023227306
 */
public class DownloaderWorker implements Runnable {
    
    private final int workerId;
    private final URLQueueInterface urlQueue; 
    private final RobotsTxtParser robotsParser;
    private final ReliableMulticast multicast;
    private volatile boolean running = true;
    
    /**
     * Construtor do Worker.
     *
     * @param workerId Identificador numérico do worker (para logs).
     * @param urlQueue Interface RMI para comunicar com a fila de URLs central.
     * @param robotsParser Parser para verificar permissões de acesso (robots.txt).
     * @param multicast Instância do protocolo multicast para envio de dados aos Barrels.
     */
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
                // 1. Obter URL da Queue Remota
                String url = null;
                try {
                    url = urlQueue.getNextURL();
                } catch (RemoteException e) {
                    System.err.println("Worker " + workerId + " - Erro ao contactar Queue: " + e.getMessage());
                    Thread.sleep(5000); // Espera um pouco se a Queue falhar antes de tentar de novo
                    continue;
                }
                
                if (url == null) {
                    Thread.sleep(1000); // Fila vazia, aguardar
                    continue;
                }
                
                // 2. Verificar permissões (Robots.txt)
                if (!robotsParser.isAllowed(url)) {
                    System.out.println("Worker " + workerId + " - Bloqueado por robots.txt: " + url);
                    try {
                        urlQueue.markAsVisited(url);
                    } catch (RemoteException e) { /* Ignorar erro de conexão aqui */ }
                    continue;
                }
                
                // 3. Politeness (Crawl Delay)
                long crawlDelay = robotsParser.getCrawlDelay(url);
                if (crawlDelay > 0) {
                    Thread.sleep(crawlDelay);
                }
                
                System.out.println("Worker " + workerId + " processando: " + url);
                
                // 4. Download e Parsing
                PageInfo pageInfo = downloadAndParse(url);
                
                if (pageInfo != null) {
                    try {
                        urlQueue.markAsVisited(url);
                        
                        // 5. Enviar novos links para a Queue
                        for (String newUrl : pageInfo.getLinks()) {
                            urlQueue.addURL(newUrl);
                        }
                    } catch (RemoteException e) {
                        System.err.println("Worker " + workerId + " - Erro ao atualizar Queue: " + e.getMessage());
                    }
                    
                    // 6. Enviar dados processados para os Barrels (Multicast)
                    if (multicast != null) {
                        try {
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
    
    /**
     * Realiza o download da página e extrai a informação relevante.
     *
     * @param url O endereço Web a descarregar.
     * @return Objeto {@link PageInfo} com os dados extraídos, ou null se houver erro.
     */
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
            
            // Cria um snippet (citação) curto
            String citation = text.length() > 150 
                ? text.substring(0, 150) + "..." 
                : text;
            
            return new PageInfo(url, title, citation, words, links);
            
        } catch (IOException e) {
            // Erros de IO (404, timeout) são comuns na web, retornamos null para seguir em frente
            return null;
        }
    }
    
    /**
     * Extrai palavras do texto, limpando caracteres especiais e números.
     * Suporta caracteres Unicode (acentos, alfabetos não latinos).
     *
     * @param text O texto puro da página.
     * @return Conjunto de palavras únicas normalizadas.
     */
    private Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();
        
        // Regex Unicode: \p{L} apanha qualquer letra em qualquer língua
        String[] tokens = text.toLowerCase().split("[^\\p{L}\\p{N}]+");
        
        for (String token : tokens) {
            // Aceita palavras com 2 ou mais letras (ex: "bi", "ai", "uc")
            if (token.length() >= 2) { 
                words.add(token);
            }
        }
        return words;
    }
    
    /**
     * Extrai todos os hiperlinks (tags 'a') do documento.
     *
     * @param doc O documento HTML parseado pelo Jsoup.
     * @return Lista de URLs absolutos encontrados.
     */
    private List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements linkElements = doc.select("a[href]");
        
        for (Element link : linkElements) {
            String href = link.attr("abs:href");
            // Filtra apenas protocolos HTTP/HTTPS
            if (href.startsWith("http://") || href.startsWith("https://")) {
                links.add(href);
            }
        }
        return links;
    }

    /**
     * Sinaliza o worker para parar a execução de forma segura.
     */
    public void stop() {
        running = false;
    }
}