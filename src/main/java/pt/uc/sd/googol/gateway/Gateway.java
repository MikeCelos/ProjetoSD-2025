package pt.uc.sd.googol.gateway;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;
import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.common.SearchResult;


public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private final List<BarrelInterface> barrels = new ArrayList<>();
    private int current = 0; // para round-robin

    public Gateway(List<String> barrelHosts) throws RemoteException {
        super();
        // ligar a cada barrel remoto
        for (String host : barrelHosts) {
            try {
                BarrelInterface b = (BarrelInterface) Naming.lookup("//" + host + "/barrel");
                barrels.add(b);
            } catch (Exception e) {
                System.err.println("Erro ao ligar ao barrel: " + host);
            }
        }
    }

    @Override
    public List<SearchResult> search(String query) throws RemoteException {
        if (barrels.isEmpty()) throw new RemoteException("Nenhum barrel disponível");

        // tokenizar UMA vez antes do loop
        List<String> terms =
            java.util.Arrays.stream(query.toLowerCase().split("\\W+"))
                            .filter(s -> !s.isBlank())
                            .toList(); // se teu Java <16, usa Collectors.toList()

        RemoteException last = null;
        for (int i = 0; i < barrels.size(); i++) {
            BarrelInterface barrel = barrels.get(current);
            current = (current + 1) % barrels.size(); // round-robin
            try {
                return barrel.searchAllTerms(terms, 0, 10); // método correto no Barrel
            } catch (RemoteException e) {
                System.err.println("Barrel falhou, tentando outro... " + e.getMessage());
                last = e;
            }
        }
        throw (last != null) ? last : new RemoteException("Todos os barrels falharam.");
    }

    @Override
    public String addURL(String url) throws RemoteException {
        // Opcional: enviar para um downloader ou barrel
        return "URL recebido: " + url;
    }

    public static void main(String[] args) {
        try {
            List<String> hosts = List.of("localhost:1099", "localhost:1100");
            Gateway gateway = new Gateway(hosts);

            Registry registry = LocateRegistry.createRegistry(2000);
            registry.rebind("gateway", gateway);
            System.out.println("Gateway pronta.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}