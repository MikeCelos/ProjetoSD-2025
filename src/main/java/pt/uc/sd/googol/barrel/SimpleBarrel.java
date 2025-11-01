package pt.uc.sd.googol.barrel;

import pt.uc.sd.googol.common.PageInfo;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleBarrel extends UnicastRemoteObject implements BarrelInterface {
    
    private final Map<String, PageInfo> pages = new ConcurrentHashMap<>();
    
    protected SimpleBarrel() throws RemoteException {
        super();
    }
    
    @Override
    public void addDocument(PageInfo page) throws RemoteException {
        System.out.println("📥 Recebido: " + page.getUrl());
        pages.put(page.getUrl(), page);
        System.out.println("✓ Total indexado: " + pages.size() + " páginas");
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
            
            // Manter vivo
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
