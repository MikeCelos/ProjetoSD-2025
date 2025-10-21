package gateway;

import search.Index; // a tua interface remota do exercício

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayServer extends UnicastRemoteObject implements Gateway {
    // lista de barrels (stubs RMI)
    private final List<Index> barrels = Collections.synchronizedList(new ArrayList<>());
    private int rr = -1; // round-robin pointer

    // estatísticas
    private final Map<String,Integer> topQueries = new ConcurrentHashMap<>();
    private final Map<String,Long> latSum = new ConcurrentHashMap<>(); // rmiUrl -> soma latências
    private final Map<String,Integer> latN = new ConcurrentHashMap<>(); // rmiUrl -> nº chamadas

    public GatewayServer() throws RemoteException { super(); }

    // ========================= API RMI =========================

    @Override
    public synchronized void registerBarrel(String rmiUrl) throws RemoteException {
        try {
            Index idx = (Index) Naming.lookup(rmiUrl);
            barrels.add(idx);
            System.out.println("Gateway: registado barrel " + rmiUrl);
            // inicializa métricas
            latSum.putIfAbsent(rmiUrl, 0L);
            latN.putIfAbsent(rmiUrl, 0);
        } catch (Exception e) {
            throw new RemoteException("Falha a ligar a " + rmiUrl, e);
        }
    }

    @Override
    public List<String> search(String query, int page) throws RemoteException {
        // contabiliza top-10
        String qKey = normalize(query);
        topQueries.merge(qKey, 1, Integer::sum);

        List<String> terms = tokenize(qKey);
        if (terms.isEmpty()) return List.of();

        // estratégia de agregação:
        // 1) para cada termo, UNION dos resultados de TODOS os barrels
        // 2) no fim, AND (interseção) entre termos
        Map<String, Set<String>> perTermUnion = new HashMap<>();
        List<Index> snapshot = snapshotBarrels();

        if (snapshot.isEmpty()) throw new RemoteException("Sem barrels registados");

        for (String term : terms) {
            Set<String> union = new HashSet<>();
            for (Index b : snapshot) {
                String tag = stubTag(b);
                long t0 = System.nanoTime();
                try {
                    List<String> hits = b.searchWord(term);
                    union.addAll(hits);
                    // métrica de latência
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    latSum.merge(tag, ms, Long::sum);
                    latN.merge(tag, 1, Integer::sum);
                } catch (Exception e) {
                    // failover “natural”: se um barrel falha, seguimos para o próximo
                }
            }
            perTermUnion.put(term, union);
        }

        // interseção entre termos
        Set<String> acc = null;
        for (String term : terms) {
            Set<String> s = perTermUnion.getOrDefault(term, Set.of());
            if (acc == null) acc = new HashSet<>(s); else acc.retainAll(s);
            if (acc.isEmpty()) break;
        }
        if (acc == null || acc.isEmpty()) return List.of();

        // paginação 10 por página
        List<String> all = new ArrayList<>(acc);
        // TODO (quando o Elemento 1 tiver backlinks): ordenar por nº de ligações recebidas
        Collections.sort(all); // por agora, ordem alfabética estável
        int pageSize = 10;
        int from = Math.min(page * pageSize, all.size());
        int to   = Math.min(from + pageSize, all.size());
        return all.subList(from, to);
    }

    @Override
    public Map<String, Object> stats() throws RemoteException {
        Map<String,Object> s = new LinkedHashMap<>();
        // top-10 queries
        LinkedHashMap<String,Integer> top10 = new LinkedHashMap<>();
        topQueries.entrySet().stream()
                .sorted((a,b)-> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> top10.put(e.getKey(), e.getValue()));
        s.put("topQueries", top10);

        // latências médias por barrel (ms)
        LinkedHashMap<String,Double> latAvg = new LinkedHashMap<>();
        for (String tag : latSum.keySet()) {
            long sum = latSum.getOrDefault(tag, 0L);
            int n = latN.getOrDefault(tag, 0);
            latAvg.put(tag, n==0 ? 0.0 : sum / (double) n);
        }
        s.put("latencyMsPerBarrel", latAvg);

        // TODO (quando houver suporte no IndexServer):
        // - s.put("barrelSizes", ...);  // requer Index.docsCount()
        // - s.put("totalDocs", ...);    // soma dos tamanhos
        return s;
    }

    // ========================= helpers =========================

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private static List<String> tokenize(String q) {
        if (q.isBlank()) return List.of();
        return Arrays.stream(q.split("[^\\p{L}\\p{N}]+"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private List<Index> snapshotBarrels() {
        synchronized (barrels) { return List.copyOf(barrels); }
    }

    // tag de identificação estável para métricas (host:port/name)
    private static String stubTag(Index b) {
        // nota: Naming.lookup(url) devolve um proxy; não há API p/ ler o URL.
        // aqui usamos toString() como tag estável o suficiente em local.
        return String.valueOf(b);
    }

    // ========================= MAIN =========================

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 19999;
        try { LocateRegistry.createRegistry(port); } catch (ExportException ignore) {}
        GatewayServer gw = new GatewayServer();
        Naming.rebind("rmi://localhost:" + port + "/gateway", gw);
        System.out.println("Gateway @ rmi://localhost:" + port + "/gateway");
        System.out.println("No cliente: register rmi://HOST:8183/index ; search <termos> ; stats");
    }
}
