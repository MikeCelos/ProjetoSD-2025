package pt.uc.sd.googol.client;

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Cliente RMI para o sistema de motor de busca Googol.
 * <p>
 * Esta classe fornece uma interface de linha de comandos (CLI) para os utilizadores
 * interagirem com o sistema. Liga-se ao Gateway via RMI e permite realizar
 * pesquisas, consultar backlinks, ver estatísticas e submeter novos URLs para indexação.
 *
 * @author André Ramos 2023227306
 */
public class GoogolClient {
    
    private final GatewayInterface gateway;
    private final Scanner scanner;
    
    /**
     * Construtor do cliente Googol.
     * Estabelece a ligação RMI com o Gateway.
     *
     * @param host O endereço do host onde o RMI Registry do Gateway está a correr.
     * @param port A porta do RMI Registry.
     * @throws Exception Se ocorrer um erro ao localizar o Registry ou ao fazer lookup do Gateway.
     */
    public GoogolClient(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.gateway = (GatewayInterface) registry.lookup("gateway");
        this.scanner = new Scanner(System.in);
        
        // Testar conexão
        String ping = gateway.ping();
        System.out.println("Conectado ao Gateway: " + ping);
    }
    
    /**
     * Método principal de execução do cliente.
     * Apresenta o menu interativo e processa as escolhas do utilizador num loop contínuo.
     */
    public void run() {
        System.out.println(" GOOGOL - Cliente RMI ");
        
        while (true) {
            System.out.println("\n=== Menu ===");
            System.out.println("1. Pesquisar");
            System.out.println("2. Ver backlinks");
            System.out.println("3. Estatísticas");
            System.out.println("4. Indexar novo URL (Admin)"); // <--- NOVA OPÇÃO
            System.out.println("5. Sair");
            System.out.print("\nEscolha uma opção: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1": search(); break;
                case "2": backlinks(); break;
                case "3": stats(); break;
                case "4": indexNewUrl(); break; // <--- CHAMADA
                case "5": 
                    System.out.println("Até logo!");
                    return;
                default: System.out.println("Opção inválida!");
            }
        }
    }
    
    /**
     * Realiza uma pesquisa interativa no sistema.
     * Solicita termos de pesquisa ao utilizador e apresenta os resultados paginados.
     * Permite navegar entre páginas de resultados (Próxima/Anterior).
     */
    private void search() {
        try {
            System.out.print("\n Digite os termos de pesquisa (separados por espaço): ");
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
                        System.out.println("\n Nenhum resultado encontrado.");
                    } else {
                        System.out.println("\n Não há mais resultados.");
                    }
                    break;
                }
                
                System.out.println("\n Resultados (Página " + (currentPage + 1) + "):");
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
            System.err.println(" Erro na pesquisa: " + e.getMessage());
        }
    }
    
    /**
     * Consulta e apresenta os backlinks para um determinado URL.
     * Solicita um URL e lista todas as páginas indexadas que apontam para ele.
     */
    private void backlinks() {
        try {
            System.out.print("\n Digite o URL: ");
            String url = scanner.nextLine().trim();
            
            List<String> backlinks = gateway.getBacklinks(url);
            
            if (backlinks.isEmpty()) {
                System.out.println("\n Nenhum backlink encontrado para: " + url);
            } else {
                System.out.println("\n Páginas que apontam para " + url + ":");
                System.out.println("═══════════════════════════════════════");
                for (String link : backlinks) {
                    System.out.println("  • " + link);
                }
                System.out.println("\nTotal: " + backlinks.size() + " backlinks");
            }
            
        } catch (Exception e) {
            System.err.println(" Erro ao obter backlinks: " + e.getMessage());
        }
    }
    
    /**
     * Obtém e apresenta estatísticas gerais do sistema.
     * Inclui informações sobre Barrels ativos, top de pesquisas e tempos de resposta.
     */
    private void stats() {
        try {
            String stats = gateway.getStats();
            System.out.println("\n" + stats);
        } catch (Exception e) {
            System.err.println(" Erro ao obter estatísticas: " + e.getMessage());
        }
    }
    
    /**
     * Ponto de entrada da aplicação cliente.
     * Inicializa o cliente e arranca a interface.
     *
     * @param args Argumentos da linha de comando (não utilizados atualmente).
     */
    public static void main(String[] args) {
        try {
            GoogolClient client = new GoogolClient("localhost", 1100);
            client.run();
        } catch (Exception e) {
            System.err.println(" Erro ao conectar ao Gateway:");
            e.printStackTrace();
        }
    }

    /**
     * Permite ao utilizador submeter manualmente um novo URL para ser indexado.
     * O URL é enviado para a fila de processamento com prioridade.
     */
    private void indexNewUrl() {
        try {
            System.out.print("\n Digite o URL para indexar: ");
            String url = scanner.nextLine().trim();
            
            if (url.isEmpty()) return;
            
            boolean sucesso = gateway.indexUrl(url);
            
            if (sucesso) {
                System.out.println(" URL adicionado à fila de processamento!");
                System.out.println("   O Downloader irá visitá-lo em breve.");
            } else {
                System.out.println(" Erro ao adicionar URL (Gateway pode estar sem conexão à Queue).");
            }
            
        } catch (Exception e) {
            System.err.println(" Erro: " + e.getMessage());
        }
    }
}