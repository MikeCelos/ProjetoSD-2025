package pt.uc.sd.googol.downloader;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class QueueServer {
    
    public static void main(String[] args) {
        try {
            int port = 1098; // Porta diferente dos Barrels (1099) para evitar conflitos locais
            
            // Cria o Registry
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("RMI Registry criado na porta " + port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Usando RMI Registry existente na porta " + port);
            }
            
            // Instancia e regista a Queue
            URLQueue queue = new URLQueue();
            registry.rebind("queue", queue);
            
            System.out.println("=== URL Queue Server Pronto ===");
            System.out.println("À espera de Downloaders...");
            
            
        } catch (Exception e) {
            System.err.println("Erro no QueueServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}