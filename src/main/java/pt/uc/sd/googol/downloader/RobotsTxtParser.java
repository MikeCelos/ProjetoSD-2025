package pt.uc.sd.googol.downloader;

import org.jsoup.Jsoup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsTxtParser {
    
    // Cache de robots.txt por domínio
    private final Map<String, RobotRules> robotsCache;
    private final String userAgent;
    
    public RobotsTxtParser(String userAgent) {
        this.userAgent = userAgent;
        this.robotsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Verifica se um URL pode ser crawled de acordo com robots.txt
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
     * Obtém o crawl delay em milissegundos para um domínio
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
     * Faz download e parse do robots.txt de um domínio
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
     * Faz parse do conteúdo do robots.txt
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
     * Classe interna que representa as regras de um robots.txt
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
         * Verifica se um path é permitido
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
     * Limpa o cache (útil para testes)
     */
    public void clearCache() {
        robotsCache.clear();
    }
    
    /**
     * Retorna estatísticas do cache
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