package pt.uc.sd.googol.client;

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class GoogolClient {
    
    private final GatewayInterface gateway;
    private final Scanner scanner;
    
    public GoogolClient(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.gateway = (GatewayInterface) registry.lookup("gateway");
        this.scanner = new Scanner(System.in);
        
        // Testar conexão
        String ping = gateway.ping();
        System.out.println("✓ Conectado ao Gateway: " + ping);
    }
    
    public void run() {
        System.out.println("\n╔════════════════════════════════╗");
        System.out.println("║      GOOGOL - Cliente RMI      ║");
        System.out.println("╚════════════════════════════════╝\n");
        
        while (true) {
            System.out.println("\n=== Menu ===");
            System.out.println("1. Pesquisar");
            System.out.println("2. Ver backlinks");
            System.out.println("3. Estatísticas");
            System.out.println("4. Sair");
            System.out.print("\nEscolha uma opção: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    search();
                    break;
                case "2":
                    backlinks();
                    break;
                case "3":
                    stats();
                    break;
                case "4":
                    System.out.println("Até logo!");
                    return;
                default:
                    System.out.println("Opção inválida!");
            }
        }
    }
    
    private void search() {
        try {
            System.out.print("\n🔍 Digite os termos de pesquisa (separados por espaço): ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                System.out.println("Por favor, digite algo!");
                return;
            }
            
            List<String> terms = Arrays.asList(input.split("\\s+"));
            int currentPage = 0;
            
            while (true) {
                List<SearchResult> results = gateway.search(terms, currentPage);
                
                if (results.isEmpty()) {
                    if (currentPage == 0) {
                        System.out.println("\n❌ Nenhum resultado encontrado.");
                    } else {
                        System.out.println("\n📭 Não há mais resultados.");
                    }
                    break;
                }
                
                System.out.println("\n📊 Resultados (Página " + (currentPage + 1) + "):");
                System.out.println("═══════════════════════════════════════");
                
                for (int i = 0; i < results.size(); i++) {
                    System.out.println("\n" + (i + 1) + ". " + results.get(i));
                }
                
                System.out.println("\n[N]próxima | [V]oltar | [S]air: ");
                String nav = scanner.nextLine().trim().toLowerCase();
                
                if (nav.equals("n")) {
                    currentPage++;
                } else if (nav.equals("v") && currentPage > 0) {
                    currentPage--;
                } else {
                    break;
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro na pesquisa: " + e.getMessage());
        }
    }
    
    private void backlinks() {
        try {
            System.out.print("\n🔗 Digite o URL: ");
            String url = scanner.nextLine().trim();
            
            List<String> backlinks = gateway.getBacklinks(url);
            
            if (backlinks.isEmpty()) {
                System.out.println("\n❌ Nenhum backlink encontrado para: " + url);
            } else {
                System.out.println("\n📎 Páginas que apontam para " + url + ":");
                System.out.println("═══════════════════════════════════════");
                for (String link : backlinks) {
                    System.out.println("  • " + link);
                }
                System.out.println("\nTotal: " + backlinks.size() + " backlinks");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao obter backlinks: " + e.getMessage());
        }
    }
    
    private void stats() {
        try {
            String stats = gateway.getStats();
            System.out.println("\n" + stats);
        } catch (Exception e) {
            System.err.println("❌ Erro ao obter estatísticas: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            GoogolClient client = new GoogolClient("localhost", 1100);
            client.run();
        } catch (Exception e) {
            System.err.println("❌ Erro ao conectar ao Gateway:");
            e.printStackTrace();
        }
    }
}