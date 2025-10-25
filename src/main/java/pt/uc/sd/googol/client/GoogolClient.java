// src/main/java/pt/uc/sd/googol/client/GoogolClient.java
package pt.uc.sd.googol.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

import pt.uc.sd.googol.common.SearchResult;
import pt.uc.sd.googol.gateway.GatewayInterface;

public class GoogolClient {
    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 2000;

        try {
            Registry reg = LocateRegistry.getRegistry(host, port);
            GatewayInterface gateway = (GatewayInterface) reg.lookup("gateway");

            try (Scanner sc = new Scanner(System.in)) {
                while (true) {
                    System.out.println("\n--- GOOGOL ---");
                    System.out.println("1. Pesquisar");
                    System.out.println("2. Adicionar URL");
                    System.out.println("3. Sair");
                    System.out.print("Escolha: ");

                    String line = sc.nextLine().trim();
                    int opt;
                    try { opt = Integer.parseInt(line); } catch (NumberFormatException e) { continue; }

                    switch (opt) {
                        case 1 -> {
                            System.out.print("Query: ");
                            String query = sc.nextLine().trim();
                            if (query.isBlank()) { System.out.println("Query vazia."); break; }

                            List<SearchResult> results = gateway.search(query); // <- SEM paginação
                            if (results == null || results.isEmpty()) {
                                System.out.println("Sem resultados.");
                                break;
                            }

                            int n = 1;
                            System.out.println("\nResultados:");
                            for (SearchResult r : results) {
                                System.out.printf("%2d) %s (rank=%d)\n    %s\n    %s\n\n",
                                        n++, r.getTitle(), r.getRank(), r.getUrl(), r.getSnippet());
                            }
                        }

                        case 2 -> {
                            System.out.print("URL a adicionar: ");
                            String url = sc.nextLine().trim();
                            if (url.isBlank()) { System.out.println("URL vazio."); break; }
                            String resp = gateway.addURL(url); // <- usa tua assinatura atual
                            System.out.println(resp != null ? resp : "URL recebido.");
                        }

                        case 3 -> {
                            System.out.println("A sair...");
                            return;
                        }

                        default -> System.out.println("Opção inválida.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
