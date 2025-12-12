package pt.uc.sd.googol.barrel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;

/**
 * Implementação do servidor de armazenamento (Storage Barrel).
 * <p>
 * Esta classe gere o índice invertido, o armazenamento de páginas e os backlinks.
 * Implementa mecanismos de tolerância a falhas, incluindo:
 * <ul>
 * <li>Persistência de dados em disco (ficheiros .dat).</li>
 * <li>Sincronização automática com outros Barrels no arranque (State Transfer).</li>
 * <li>Thread de auto-save para salvaguarda periódica.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
public class SimpleBarrel extends UnicastRemoteObject implements BarrelInterface {
    
    private final int barrelId;
    private final String dataFileName;
    
    // Mapas iniciados imediatamente para evitar NullPointer
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> backlinks = new ConcurrentHashMap<>();
    
    // Flag de segurança para evitar gravar dados incompletos durante o arranque
    private volatile boolean isReady = false;
    
    /**
     * Construtor do Barrel.
     * Inicia o processo de recuperação de dados (Sincronização ou Disco) numa thread separada
     * para não bloquear o registo RMI.
     *
     * @param barrelId Identificador único deste Barrel (0, 1, etc.).
     * @throws RemoteException Se houver erro na exportação RMI.
     */
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
        
        // Shutdown Hook para gravar ao fechar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isReady) saveToDisk();
        }));
    }

    /**
     * Tenta sincronizar dados a partir de outro Barrel ativo na rede.
     * Procura outros serviços "barrelX" no RMI Registry.
     *
     * @return true se a sincronização foi bem sucedida, false caso contrário.
     */
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

    /**
     * Grava o estado atual dos mapas para um ficheiro local (.dat).
     * Só executa se a flag isReady for verdadeira.
     */
    private synchronized void saveToDisk() {
        if (!isReady) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFileName))) {
            oos.writeObject(pages);
            oos.writeObject(index);
            oos.writeObject(backlinks);
        } catch (IOException e) {
            System.err.println(" [Disk] Erro ao gravar: " + e.getMessage());
        }
    }
    
    /**
     * Carrega o estado a partir do ficheiro local (.dat).
     */
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
            
            this.pages.putAll(p);
            this.index.putAll(i);
            this.backlinks.putAll(b);
            
            System.out.println(" [Disk] Dados carregados do disco com sucesso.");
        } catch (Exception e) {
            System.err.println(" [Disk] Erro ao ler ficheiro: " + e.getMessage());
        }
    }

    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        pages.put(page.getUrl(), page);
        
        for (String word : page.getWords()) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        for (String link : page.getLinks()) {
            backlinks.computeIfAbsent(link, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        
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