package pt.uc.sd.googol.barrel;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.common.SearchResult;

public class IndexStorageBarrel extends UnicastRemoteObject implements BarrelInterface {

    // inverted index: termo -> set de urls
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    // metadados por url
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    // backlinks (para ranking por in-degree)
    private final Map<String, Set<String>> backlinks = new ConcurrentHashMap<>();

    protected IndexStorageBarrel() throws RemoteException { super(); }


    public void addDocument(PageInfo page) throws RemoteException {
        pages.put(page.getUrl(), page);
        for (String t : page.getWords()) {
            index.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
        // atualizar backlinks a partir dos outLinks deste page (cada outLink recebe um backlink deste URL)
        for (String out : page.getLinks()) {
            backlinks.computeIfAbsent(out, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
        }
    }

    @Override public void addBacklinks(String url, List<String> incoming) {
        backlinks.computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet()).addAll(incoming);
    }

    @Override
    public List<SearchResult> searchAllTerms(List<String> terms, int page, int pageSize) throws RemoteException {
    if (terms.isEmpty()) return List.of();

    // interseção dos conjuntos
    Set<String> acc = null;
    for (String t : terms) {
        Set<String> s = index.getOrDefault(t, Set.of());
        acc = (acc == null) ? new HashSet<>(s) : intersect(acc, s);
        if (acc.isEmpty()) break;
    }
    if (acc == null || acc.isEmpty()) return List.of();

    // ranking por in-degree (nº de backlinks)
    List<String> urls = new ArrayList<>(acc);
    urls.sort(Comparator.comparingInt(u -> -backlinks.getOrDefault(u, Set.of()).size()));

    // paginação (10 por página)
    int from = page * pageSize;
    int to = Math.min(from + pageSize, urls.size());
    if (from >= urls.size()) return List.of();

    List<SearchResult> out = new ArrayList<>();
    for (String u : urls.subList(from, to)) {
        PageInfo p = pages.get(u);
        int indeg = backlinks.getOrDefault(u, Set.of()).size();
        out.add(new SearchResult(u, p != null ? p.getTitle() : u, snippetOf(p), indeg));
    }
    return out;
}


    @Override public List<String> getBacklinks(String url) { 
        return new ArrayList<>(backlinks.getOrDefault(url, Set.of())); 
    }

    @Override public String ping() { return "OK"; }

    @Override public Map<String, Object> stats() {
        return Map.of(
            "terms", index.size(),
            "pages", pages.size(),
            "backlinkEntries", backlinks.size()
        );
    }

    private static Set<String> intersect(Set<String> a, Set<String> b) {
        if (a.size() > b.size()) { var t = a; a = b; b = t; }
        Set<String> r = new HashSet<>();
        for (String x : a) if (b.contains(x)) r.add(x);
        return r;
    }

    private static String snippetOf(PageInfo p) { return p != null ? p.getCitation() : ""; }

    public static void main(String[] args) {
        try {
            int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1099;
            Registry reg = LocateRegistry.createRegistry(port);
            IndexStorageBarrel barrel = new IndexStorageBarrel();
            reg.rebind("barrel", barrel);
            System.out.println("Barrel @ RMI registry port " + port);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
