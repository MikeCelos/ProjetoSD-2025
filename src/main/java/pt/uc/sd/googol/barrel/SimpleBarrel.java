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
    
    // Mapas iniciados imediatamente para evitar NullPointer
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> backlinks = new ConcurrentHashMap<>();
    
    // Flag de segurança para evitar gravar dados incompletos
    private volatile boolean isReady = false;
    
    protected SimpleBarrel(int barrelId) throws RemoteException {
        super();
        this.barrelId = barrelId;
        this.dataFileName = "barrel" + barrelId + ".dat";
        
        // Thread de Inicialização (Sync + Load)
        new Thread(() -> {
            System.out.println(" [Barrel" + barrelId + "] A iniciar processo de recuperação...");
            
            // 1. Tentar Sincronizar de outro par (P2P)
            boolean synced = syncFromPeer();
            
            // 2. Se a sincronização falhar, tentar ler do disco local
            if (!synced) {
                loadFromDisk();
            }
            
            // Marcar como pronto! A partir de agora podemos gravar em disco.
            isReady = true;
            System.out.println(" [Barrel" + barrelId + "] ESTADO: ONLINE E PRONTO (Total: " + pages.size() + " páginas)");
            
            // Forçar uma gravação imediata para garantir consistência
            saveToDisk();
            
        }).start();

        // Thread de Auto-Save (a cada 10s)
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    if (isReady) saveToDisk(); // Só grava se estiver pronto
                } catch (InterruptedException e) { break; }
            }
        }).start();
        
        // Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isReady) saveToDisk();
        }));
    }

    private boolean syncFromPeer() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            String[] list = registry.list();
            
            for (String name : list) {
                if (name.startsWith("barrel") && !name.equals("barrel" + barrelId)) {
                    try {
                        System.out.println(" [Sync] Tentando copiar de " + name + "...");
                        BarrelInterface peer = (BarrelInterface) registry.lookup(name);
                        
                        SyncData data = peer.getFullState();
                        if (data != null) {
                            // USAR putAll PARA NÃO PERDER DADOS RECEBIDOS VIA MULTICAST DURANTE O ARRANQUE
                            this.pages.putAll(data.pages);
                            this.index.putAll(data.index);
                            this.backlinks.putAll(data.backlinks);
                            
                            System.out.println(" [Sync] SUCESSO! Sincronizado com " + name);
                            return true;
                        }
                    } catch (Exception e) {
                        System.err.println(" [Sync] Falha ao copiar de " + name + " (tentando próximo...)");
                    }
                }
            }
        } catch (Exception e) { }
        System.out.println(" [Sync] Nenhum par disponível para sincronização.");
        return false;
    }

    @Override
    public SyncData getFullState() throws RemoteException {
        // Retorna cópia dos dados atuais
        return new SyncData(new HashMap<>(pages), new HashMap<>(index), new HashMap<>(backlinks));
    }

    private synchronized void saveToDisk() {
        // SEGURANÇA: Nunca gravar se ainda estamos a carregar!
        // Isto impede que um barrel vazio grave um ficheiro vazio por cima do backup.
        if (!isReady) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFileName))) {
            oos.writeObject(pages);
            oos.writeObject(index);
            oos.writeObject(backlinks);
        } catch (IOException e) {
            System.err.println(" [Disk] Erro ao gravar: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        File file = new File(dataFileName);
        if (!file.exists()) {
            System.out.println(" [Disk] Nenhum ficheiro local encontrado.");
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, PageInfo> p = (Map<String, PageInfo>) ois.readObject();
            Map<String, Set<String>> i = (Map<String, Set<String>>) ois.readObject();
            Map<String, Set<String>> b = (Map<String, Set<String>>) ois.readObject();
            
            // Usar putAll também aqui
            this.pages.putAll(p);
            this.index.putAll(i);
            this.backlinks.putAll(b);
            
            System.out.println(" [Disk] Dados carregados do disco com sucesso.");
        } catch (Exception e) {
            System.err.println(" [Disk] Erro ao ler ficheiro: " + e.getMessage());
        }
    }

    // --- Métodos Remotos ---
    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        pages.put(page.getUrl(), page);
        
        // Indexação manual para garantir thread-safety nos Sets internos
        for (String word : page.getWords()) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        for (String link : page.getLinks()) {
            backlinks.computeIfAbsent(link, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
        // Feedback visual reduzido para não poluir logs
        if (pages.size() % 10 == 0) { 
            System.out.println(" [Barrel" + barrelId + "] Total: " + pages.size());
        }
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
}