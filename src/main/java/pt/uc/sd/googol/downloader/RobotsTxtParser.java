package pt.uc.sd.googol.downloader;

import org.jsoup.Jsoup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analisador de ficheiros robots.txt (Robot Exclusion Standard).
 * <p>
 * Esta classe é responsável por verificar se o Crawler tem permissão para aceder a determinados URLs
 * e respeitar as regras de "politeness" (atrasos de rastreio) definidas pelos administradores dos sites.
 * Mantém uma cache em memória das regras por domínio para evitar downloads repetidos do mesmo robots.txt.
 *
 * @author André Ramos 2023227306
 */
public class RobotsTxtParser {
    
    /** Cache de regras (RobotRules) indexada pelo domínio (ex: http://www.uc.pt). */
    private final Map<String, RobotRules> robotsCache;
    
    /** O nome do User-Agent deste bot (ex: "Googol Bot 1.0") para verificar regras específicas. */
    private final String userAgent;
    
    /**
     * Construtor do parser.
     *
     * @param userAgent O nome do agente que será usado para identificar este Crawler nos ficheiros robots.txt.
     */
    public RobotsTxtParser(String userAgent) {
        this.userAgent = userAgent;
        this.robotsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Verifica se um URL pode ser visitado (crawled) de acordo com as regras do robots.txt do domínio.
     * Se as regras para o domínio ainda não estiverem em cache, faz o download e parse do ficheiro.
     *
     * @param url O URL completo a verificar.
     * @return true se o acesso for permitido ou se não houver robots.txt, false se for proibido.
     */
    public boolean isAllowed(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getScheme() + "://" + uri.getHost();
            
            // Buscar ou fazer cache das regras deste domínio
            RobotRules rules = robotsCache.computeIfAbsent(domain, this::fetchRobotsTxt);
            
            // Se não conseguiu buscar robots.txt, permite por padrão
            if (rules == null) {
                return true;
            }
            
            // Verifica se o path é permitido
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            
            return rules.isAllowed(path);
            
        } catch (URISyntaxException e) {
            System.err.println("URL inválido: " + url);
            return false;
        }
    }
    
    /**
     * Obtém o atraso de rastreio (crawl delay) definido para um determinado URL.
     *
     * @param url O URL para o qual se pretende saber o delay.
     * @return O tempo de espera em milissegundos (0 se não houver delay definido).
     */
    public long getCrawlDelay(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getScheme() + "://" + uri.getHost();
            
            RobotRules rules = robotsCache.get(domain);
            if (rules != null && rules.crawlDelay > 0) {
                return rules.crawlDelay * 1000; // converter para ms
            }
            
        } catch (URISyntaxException e) {
            // ignorar
        }
        
        return 0; // sem delay
    }
    
    /**
     * Faz o download e interpretação do ficheiro robots.txt de um domínio.
     * Usa a biblioteca Jsoup para fazer o pedido HTTP.
     *
     * @param domain O domínio base (ex: https://www.uc.pt).
     * @return Objeto {@link RobotRules} com as regras analisadas.
     */
    private RobotRules fetchRobotsTxt(String domain) {
        String robotsUrl = domain + "/robots.txt";
        
        try {
            System.out.println("Buscando robots.txt: " + robotsUrl);
            
            String content = Jsoup.connect(robotsUrl)
                    .timeout(5000)
                    .userAgent(userAgent)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            
            return parseRobotsTxt(content);
            
        } catch (IOException e) {
            // Se não existe robots.txt ou erro, permite tudo
            System.out.println("Sem robots.txt em " + domain + " (permite tudo)");
            return new RobotRules(true); // permite tudo por padrão
        }
    }
    
    /**
     * Analisa o conteúdo de texto de um ficheiro robots.txt e extrai as regras relevantes.
     * Processa apenas as secções que se aplicam ao User-Agent deste bot (ou "*").
     *
     * @param content O conteúdo textual do ficheiro robots.txt.
     * @return As regras extraídas.
     */
    private RobotRules parseRobotsTxt(String content) {
        RobotRules rules = new RobotRules(false);
        boolean relevantSection = false; // se estamos numa secção relevante para o nosso user-agent
        
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // Ignorar comentários e linhas vazias
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Separar chave: valor
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            
            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();
            
            // User-agent
            if (key.equals("user-agent")) {
                // Verifica se esta secção se aplica ao nosso bot
                relevantSection = value.equals("*") || 
                                value.equalsIgnoreCase(userAgent) ||
                                userAgent.toLowerCase().contains(value.toLowerCase());
            }
            
            // Se não estamos numa secção relevante, ignorar
            if (!relevantSection) {
                continue;
            }
            
            // Disallow
            if (key.equals("disallow")) {
                if (value.isEmpty()) {
                    // Disallow vazio = permite tudo
                    rules.allowAll = true;
                } else {
                    rules.disallowedPaths.add(value);
                }
            }
            
            // Allow (sobrepõe disallow)
            else if (key.equals("allow")) {
                if (!value.isEmpty()) {
                    rules.allowedPaths.add(value);
                }
            }
            
            // Crawl-delay
            else if (key.equals("crawl-delay")) {
                try {
                    rules.crawlDelay = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // ignorar se não for número
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Classe interna que armazena as regras de permissão e atraso para um domínio específico.
     */
    private static class RobotRules {
        boolean allowAll;
        List<String> disallowedPaths;
        List<String> allowedPaths;
        int crawlDelay; // em segundos
        
        RobotRules(boolean allowAll) {
            this.allowAll = allowAll;
            this.disallowedPaths = new ArrayList<>();
            this.allowedPaths = new ArrayList<>();
            this.crawlDelay = 0;
        }
        
        /**
         * Verifica se um caminho específico é permitido pelas regras armazenadas.
         * Segue a lógica padrão: Allow tem precedência sobre Disallow.
         *
         * @param path O caminho do URL (ex: /admin/login).
         * @return true se permitido, false caso contrário.
         */
        boolean isAllowed(String path) {
            // Se permite tudo
            if (allowAll) {
                return true;
            }
            
            // Primeiro verifica allow (tem precedência)
            for (String allowedPath : allowedPaths) {
                if (path.startsWith(allowedPath)) {
                    return true;
                }
            }
            
            // Depois verifica disallow
            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    return false;
                }
            }
            
            // Se não tem regras específicas, permite
            return true;
        }
    }
    
    /**
     * Limpa a cache de regras. Útil para testes ou para forçar a atualização das regras.
     */
    public void clearCache() {
        robotsCache.clear();
    }
    
    /**
     * Imprime no terminal estatísticas sobre a cache de robots.txt.
     * Mostra quantos domínios estão em cache e um resumo das regras para cada um.
     */
    public void printStats() {
        System.out.println("=== Robots.txt Cache ===");
        System.out.println("Domínios em cache: " + robotsCache.size());
        robotsCache.forEach((domain, rules) -> {
            System.out.println("  " + domain + ": " + 
                (rules.allowAll ? "permite tudo" : 
                 rules.disallowedPaths.size() + " disallow, " + 
                 rules.allowedPaths.size() + " allow"));
        });
    }
}