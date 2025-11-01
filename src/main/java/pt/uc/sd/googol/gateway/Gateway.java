package pt.uc.sd.googol.gateway;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;
import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.SearchResult;
import pt.uc.sd.googol.downloader.DownloaderInterface;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private final List<BarrelInterface> barrels = new ArrayList<>();
    private int current = 0; // para round-robin
    private DownloaderInterface downloader;

    public Gateway(List<String> barrelHosts) throws RemoteException {
        super();
        // ligar a cada barrel remoto
        for (String host : barrelHosts) {
            try {
                BarrelInterface b = (BarrelInterface) Naming.lookup("//" + host + "/barrel");
                barrels.add(b);
            } catch (Exception e) {
                System.err.println("Erro ao ligar ao barrel: " + host + " -> " + e.getMessage());
            }
        }
    }

    @Override
    public List<SearchResult> search(String query) throws RemoteException {
        if (barrels.isEmpty()) throw new RemoteException("Nenhum barrel disponível");

        List<String> terms =
            java.util.Arrays.stream(query.toLowerCase().split("\\W+"))
                            .filter(s -> !s.isBlank())
                            .toList();

        RemoteException last = null;
        for (int i = 0; i < barrels.size(); i++) {
            BarrelInterface barrel = barrels.get(current);
            current = (current + 1) % barrels.size(); // round-robin
            try {
                return barrel.searchAllTerms(terms, 0, 10);
            } catch (RemoteException e) {
                System.err.println("Barrel falhou, tentando outro... " + e.getMessage());
                last = e;
            }
        }
        throw (last != null) ? last : new RemoteException("Todos os barrels falharam.");
    }

    private synchronized void tryLookupDownloader() {
        if (this.downloader != null) return;
        try {
            this.downloader = (DownloaderInterface) Naming.lookup("//localhost:2100/downloader");
            System.out.println("[Gateway] ligado a Downloader remoto");
        } catch (Exception e) {
            System.err.println("[Gateway] não encontrou downloader remoto: " + e.getMessage());
        }
    }

    @Override
    public String addURL(String url) throws RemoteException {
        System.out.println("[Gateway] addURL chamada com: " + url);
        if (url == null) {
            throw new RemoteException("Parâmetro url é null");
        }
        if (this.downloader == null) {
            tryLookupDownloader();
        }
        if (this.downloader == null) {
            throw new RemoteException("Downloader não disponível (tente iniciar o processo downloader).");
        }
        this.downloader.addURL(url);
        return "URL enviada ao downloader: " + url;
    }

    public synchronized void setDownloader(DownloaderInterface downloader) {
        this.downloader = downloader;
    }

    public static void main(String[] args) {
        try {
            List<String> hosts = List.of("localhost:1099", "localhost:1100");
            Gateway gateway = new Gateway(hosts);

            Registry registry = LocateRegistry.createRegistry(2000);
            registry.rebind("gateway", gateway);
            System.out.println("Gateway pronta (RMI porto 2000).");
            System.out.println("[Gateway] aguarda downloader remoto em //localhost:2100/downloader (fallback: tente iniciar downloader).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}