package pt.uc.sd.googol.barrel;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BarrelLauncher {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java BarrelLauncher <barrelId>");
            System.exit(1);
        }
        
        int barrelId = Integer.parseInt(args[0]);
        
        try {
            int port = 1099;
            
            // Obter ou criar registry
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(port);
                registry.list(); // Testar se existe
                System.out.println("✓ Usando registry existente na porta " + port);
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("✓ Registry criado na porta " + port);
            }
            
            // Criar e registrar este barrel
            SimpleBarrel barrel = new SimpleBarrel(barrelId);
            registry.rebind("barrel" + barrelId, barrel);
            System.out.println("✓ Barrel" + barrelId + " rodando");
            
            // Manter vivo
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}