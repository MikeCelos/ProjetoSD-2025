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
    
    private final int barrelId;
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> backlinks = new ConcurrentHashMap<>();
    
    protected SimpleBarrel(int barrelId) throws RemoteException {
        super();
        this.barrelId = barrelId;
    }
    
    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        System.out.println(" [Barrel" + barrelId + "] Recebido: " + page.getUrl());
        
        pages.put(page.getUrl(), page);
        
        for (String word : page.getWords()) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
        for (String link : page.getLinks()) {
            backlinks.computeIfAbsent(link, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
        System.out.println(" [Barrel" + barrelId + "] Total: " + pages.size() + " páginas");
    }
    
    @Override
    public List<SearchResult> search(List<String> terms, int page) throws RemoteException {
        System.out.println(" [Barrel" + barrelId + "] Pesquisando: " + terms);
        
        if (terms.isEmpty()) return new ArrayList<>();
        
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
        
        List<String> sortedUrls = resultUrls.stream()
            .sorted((u1, u2) -> Integer.compare(
                backlinks.getOrDefault(u2, Collections.emptySet()).size(),
                backlinks.getOrDefault(u1, Collections.emptySet()).size()
            ))
            .collect(Collectors.toList());
        
        int pageSize = 10;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, sortedUrls.size());
        
        if (start >= sortedUrls.size()) {
            return new ArrayList<>();
        }
        
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
        return String.format("[Barrel%d] Páginas: %d | Termos: %d | Backlinks: %d",
            barrelId, pages.size(), index.size(), backlinks.size());
    }
    
    @Override
    public String ping() throws RemoteException {
        return "PONG from Barrel" + barrelId;
    }
    
    // ===== MAIN: Inicia múltiplos barrels =====
    public static void main(String[] args) {
        try {
            int port = 1099;
            int numBarrels = 2; // ← Configurável
            
            // Criar registry (só uma vez)
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("Registry criado na porta " + port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Registry existente na porta " + port);
            }
            
            // Criar e registrar múltiplos barrels
            for (int i = 0; i < numBarrels; i++) {
                SimpleBarrel barrel = new SimpleBarrel(i);
                registry.rebind("barrel" + i, barrel);
                System.out.println(" Barrel" + i + " rodando");
            }
            
            System.out.println("\n " + numBarrels + " Barrels prontos para receber multicast!");
            
            // Manter vivo
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}