// ...existing code...
package pt.uc.sd.googol.barrel;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.Normalizer;
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

    // Normaliza um termo: lowercase, remove diacríticos, elimina não-alfabéticos
    private static String normalizeTerm(String t) {
        if (t == null) return null;
        String s = Normalizer.normalize(t.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove marcas diacríticas
        s = s.replaceAll("[^a-z\\s]", " ").trim();
        // se for frase, quebra em tokens e devolve tokens separados por espaço (mas aqui usamos por termo)
        return s.isEmpty() ? null : s;
    }

    public void addDocument(PageInfo page) throws RemoteException {
        if (page == null || page.getUrl() == null) return;
        System.out.println("[Barrel] addDocument recebido: url=" + page.getUrl() + " title=\"" + page.getTitle() + "\" words=" + (page.getWords() == null ? 0 : page.getWords().size()));
        pages.put(page.getUrl(), page);

        if (page.getWords() != null) {
            for (String raw : page.getWords()) {
                String norm = normalizeTerm(raw);
                if (norm == null) continue;
                // norm pode conter espaços se raw era frase — dividir e indexar tokens
                for (String token : norm.split("\\s+")) {
                    if (token.length() <= 2) continue;
                    index.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
                }
            }
        }

        // atualizar backlinks a partir dos outLinks deste page (cada outLink recebe um backlink deste URL)
        if (page.getLinks() != null) {
            for (String out : page.getLinks()) {
                if (out == null) continue;
                backlinks.computeIfAbsent(out, k -> ConcurrentHashMap.newKeySet()).add(page.getUrl());
            }
        }
    }

    @Override public void addBacklinks(String url, List<String> incoming) {
        if (url == null || incoming == null) return;
        backlinks.computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet()).addAll(incoming);
        System.out.println("[Barrel] addBacklinks: " + url + " <- " + incoming.size() + " links");
    }

    @Override
    public List<SearchResult> searchAllTerms(List<String> terms, int page, int pageSize) throws RemoteException {
        if (terms == null || terms.isEmpty()) return List.of();

        // normalize incoming query terms to match indexed tokens
        List<String> normTerms = new ArrayList<>();
        for (String t : terms) {
            String n = normalizeTerm(t);
            System.out.println("[debug]" + n);
            if (n == null) continue;
            for (String tok : n.split("\\s+")) {
                if (tok.length() > 2) normTerms.add(tok);
            }
        }
        if (normTerms.isEmpty()) return List.of();

        System.out.println("[Barrel] searchAllTerms terms=" + normTerms);

        // interseção dos conjuntos
        Set<String> acc = null;
        for (String t : normTerms) {
            Set<String> s = index.getOrDefault(t, Set.of());
            acc = (acc == null) ? new HashSet<>(s) : intersect(acc, s);
            if (acc.isEmpty()) break;
        }
        if (acc == null || acc.isEmpty()) {
            System.out.println("[Barrel] searchAllTerms -> 0 resultados");
            return List.of();
        }

        // ranking por in-degree (nº de backlinks)
        List<String> urls = new ArrayList<>(acc);
        urls.sort(Comparator.comparingInt(u -> -backlinks.getOrDefault(u, Set.of()).size()));

        // paginação
        int from = page * pageSize;
        int to = Math.min(from + pageSize, urls.size());
        if (from >= urls.size()) return List.of();

        List<SearchResult> out = new ArrayList<>();
        for (String u : urls.subList(from, to)) {
            PageInfo p = pages.get(u);
            int indeg = backlinks.getOrDefault(u, Set.of()).size();
            out.add(new SearchResult(u, p != null ? p.getTitle() : u, snippetOf(p), indeg));
        }

        System.out.println("[Barrel] searchAllTerms -> " + out.size() + " resultados (total matches=" + urls.size() + ")");
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
// ...existing code...