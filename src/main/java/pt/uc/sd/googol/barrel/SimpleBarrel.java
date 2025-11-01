package pt.uc.sd.googol.barrel;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SimpleBarrel extends UnicastRemoteObject implements BarrelInterface {
    
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>(); // palavra -> URLs
    private final Map<String, Set<String>> backlinks = new ConcurrentHashMap<>(); // URL -> quem aponta
    
    protected SimpleBarrel() throws RemoteException {
        super();
    }
    
    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        System.out.println("📥 Recebido: " + page.getUrl());
        
        pages.put(page.getUrl(), page);
        
        // Indexar palavras
        for (String word : page.getWords()) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
        // Indexar backlinks
        for (String link : page.getLinks()) {
            backlinks.computeIfAbsent(link, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
        System.out.println("✓ Total indexado: " + pages.size() + " páginas");
    }
    
    @Override
    public List<SearchResult> search(List<String> terms, int page) throws RemoteException {
        System.out.println("🔍 Pesquisando: " + terms);
        
        if (terms.isEmpty()) return new ArrayList<>();
        
        // Interseção de URLs que contêm TODOS os termos
        Set<String> resultUrls = null;
        
        for (String term : terms) {
            Set<String> urlsForTerm = index.getOrDefault(term.toLowerCase(), Collections.emptySet());
            
            if (resultUrls == null) {
                resultUrls = new HashSet<>(urlsForTerm);
            } else {
                resultUrls.retainAll(urlsForTerm);
            }
            
            if (resultUrls.isEmpty()) break;
        }
        
        if (resultUrls == null || resultUrls.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Ordenar por relevância (número de backlinks)
        List<String> sortedUrls = resultUrls.stream()
            .sorted((u1, u2) -> Integer.compare(
                backlinks.getOrDefault(u2, Collections.emptySet()).size(),
                backlinks.getOrDefault(u1, Collections.emptySet()).size()
            ))
            .collect(Collectors.toList());
        
        // Paginação (10 por página)
        int pageSize = 10;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, sortedUrls.size());
        
        if (start >= sortedUrls.size()) {
            return new ArrayList<>();
        }
        
        // Criar resultados
        List<SearchResult> results = new ArrayList<>();
        for (String url : sortedUrls.subList(start, end)) {
            PageInfo pageInfo = pages.get(url);
            int relevance = backlinks.getOrDefault(url, Collections.emptySet()).size();
            
            results.add(new SearchResult(
                url,
                pageInfo.getTitle(),
                pageInfo.getCitation(),
                relevance
            ));
        }
        
        return results;
    }
    
    @Override
    public List<String> getBacklinks(String url) throws RemoteException {
        return new ArrayList<>(backlinks.getOrDefault(url, Collections.emptySet()));
    }
    
    @Override
    public String getStats() throws RemoteException {
        return String.format("Páginas: %d | Termos: %d | Backlinks: %d",
            pages.size(), index.size(), backlinks.size());
    }
    
    @Override
    public String ping() throws RemoteException {
        return "PONG";
    }
    
    public static void main(String[] args) {
        try {
            int port = 1099;
            Registry registry = LocateRegistry.createRegistry(port);
            SimpleBarrel barrel = new SimpleBarrel();
            registry.rebind("barrel", barrel);
            System.out.println("✓ Barrel rodando na porta " + port);
            
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}