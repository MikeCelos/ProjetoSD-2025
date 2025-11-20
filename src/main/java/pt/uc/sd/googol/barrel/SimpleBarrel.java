package pt.uc.sd.googol.barrel;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SimpleBarrel extends UnicastRemoteObject implements BarrelInterface {
    
    private final int barrelId;
    private final String dataFileName;
    
    private Map<String, PageInfo> pages;
    private Map<String, Set<String>> index;
    private Map<String, Set<String>> backlinks;
    
    protected SimpleBarrel(int barrelId) throws RemoteException {
        super();
        this.barrelId = barrelId;
        this.dataFileName = "barrel" + barrelId + ".dat";
        
        // Tentar sincronizar ou carregar
        if (!syncFromPeer() && !loadFromDisk()) {
            this.pages = new ConcurrentHashMap<>();
            this.index = new ConcurrentHashMap<>();
            this.backlinks = new ConcurrentHashMap<>();
            System.out.println(" [Barrel" + barrelId + "] Iniciado VAZIO.");
        }

        // Thread de Auto-Save
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    saveToDisk();
                } catch (InterruptedException e) { break; }
            }
        }).start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveToDisk));
    }

    // --- Lógica de Sincronização (State Transfer) ---
    private boolean syncFromPeer() {
        System.out.println(" [Sync] Procurando outro barrel para sincronizar...");
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            String[] list = registry.list();
            
            for (String name : list) {
                // Procura barrels que não sejam "eu próprio"
                if (name.startsWith("barrel") && !name.equals("barrel" + barrelId)) {
                    try {
                        System.out.println(" [Sync] Tentando copiar de " + name + "...");
                        BarrelInterface peer = (BarrelInterface) registry.lookup(name);
                        
                        // Pede os dados todos (pode demorar se for muita coisa)
                        SyncData data = peer.getFullState();
                        
                        this.pages = new ConcurrentHashMap<>(data.pages);
                        this.index = new ConcurrentHashMap<>(data.index);
                        this.backlinks = new ConcurrentHashMap<>(data.backlinks);
                        
                        System.out.println(" [Sync] SUCESSO! Copiado de " + name);
                        System.out.println("   -> Páginas: " + pages.size());
                        saveToDisk(); // Guarda logo em disco
                        return true;
                        
                    } catch (Exception e) {
                        System.err.println(" [Sync] Falha ao copiar de " + name + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Registry pode não estar acessível ainda
        }
        System.out.println(" [Sync] Nenhum parceiro disponível.");
        return false;
    }

    // --- Implementação do método remoto para fornecer dados ---
    @Override
    public SyncData getFullState() throws RemoteException {
        System.out.println(" [Sync] Recebi pedido de sincronização. Enviando dados...");
        // Retorna cópia dos dados atuais
        return new SyncData(pages, index, backlinks);
    }

    // --- Persistência em Disco (Backup) ---
    private synchronized void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFileName))) {
            oos.writeObject(pages);
            oos.writeObject(index);
            oos.writeObject(backlinks);
        } catch (IOException e) {
            System.err.println(" Erro ao gravar disco: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean loadFromDisk() {
        File file = new File(dataFileName);
        if (!file.exists()) return false;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            this.pages = (ConcurrentHashMap<String, PageInfo>) ois.readObject();
            this.index = (ConcurrentHashMap<String, Set<String>>) ois.readObject();
            this.backlinks = (ConcurrentHashMap<String, Set<String>>) ois.readObject();
            System.out.println(" [Disk] Dados recuperados do disco.");
            return true;
        } catch (Exception e) {
            System.err.println(" [Disk] Erro ao ler ficheiro: " + e.getMessage());
            return false;
        }
    }

    // ... MÉTODOS search, addDocument, etc. IGUAIS AO ANTERIOR ...
    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        pages.put(page.getUrl(), page);
        for (String word : page.getWords()) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        for (String link : page.getLinks()) {
            backlinks.computeIfAbsent(link, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        System.out.println(" [Barrel" + barrelId + "] Indexado: " + page.getUrl());
    }

    @Override
    public List<SearchResult> search(List<String> terms, int page) throws RemoteException {
        if (terms.isEmpty()) return new ArrayList<>();
        
        Set<String> resultUrls = null;
        for (String term : terms) {
            Set<String> urls = index.getOrDefault(term.toLowerCase(), Collections.emptySet());
            if (resultUrls == null) resultUrls = new HashSet<>(urls);
            else resultUrls.retainAll(urls);
            if (resultUrls.isEmpty()) break;
        }
        
        if (resultUrls == null) return new ArrayList<>();
        
        List<String> sorted = resultUrls.stream()
            .sorted((u1, u2) -> Integer.compare(
                backlinks.getOrDefault(u2, Collections.emptySet()).size(), 
                backlinks.getOrDefault(u1, Collections.emptySet()).size()))
            .collect(Collectors.toList());
            
        int start = page * 10;
        int end = Math.min(start + 10, sorted.size());
        if (start >= sorted.size()) return new ArrayList<>();
        
        List<SearchResult> res = new ArrayList<>();
        for (String u : sorted.subList(start, end)) {
            PageInfo p = pages.get(u);
            if (p != null) res.add(new SearchResult(u, p.getTitle(), p.getCitation(), 
                backlinks.getOrDefault(u, Collections.emptySet()).size()));
        }
        return res;
    }

    @Override
    public List<String> getBacklinks(String url) throws RemoteException {
        return new ArrayList<>(backlinks.getOrDefault(url, Collections.emptySet()));
    }

    @Override
    public String getStats() throws RemoteException {
        return String.format("[Barrel%d] P:%d | T:%d | B:%d", barrelId, pages.size(), index.size(), backlinks.size());
    }

    @Override
    public String ping() throws RemoteException { return "PONG"; }
    
    public static void main(String[] args) {
        try {
            // Se usado sem Launcher
            Registry r = LocateRegistry.createRegistry(1099);
            r.rebind("barrel0", new SimpleBarrel(0));
        } catch (Exception e) {}
    }
}