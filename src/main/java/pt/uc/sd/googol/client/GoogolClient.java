package pt.uc.sd.googol.client;


public class GoogolClient {
    public static void main(String[] args) throws Exception {
        Registry reg = LocateRegistry.getRegistry("localhost", 2000);
        GatewayInterface gateway = (GatewayInterface) reg.lookup("gateway");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- GOOGOL ---");
            System.out.println("1. Pesquisar");
            System.out.println("2. Adicionar URL");
            System.out.println("3. Estatísticas");
            System.out.println("4. Sair");
            System.out.print("Escolha: ");
            int opt = Integer.parseInt(sc.nextLine());

            switch (opt) {
                case 1 -> {
                    System.out.print("Query: ");
                    String query = sc.nextLine();
                    int page = 0;
                    List<SearchResult> results;
                    do {
                        results = gateway.search(query, page);
                        if (results.isEmpty()) {
                            System.out.println("Sem resultados.");
                            break;
                        }
                        for (SearchResult r : results)
                            System.out.printf("%d. %s (%d)\n   %s\n   %s\n\n",
                                    ++page, r.title(), r.rank(), r.url(), r.snippet());
                        System.out.print("Mostrar próxima página? (s/n): ");
                        if (!sc.nextLine().equalsIgnoreCase("s")) break;
                        page++;
                    } while (true);
                }
                case 2 -> {
                    System.out.print("URL a adicionar: ");
                    gateway.submitURL(sc.nextLine());
                }
                case 3 -> {
                    Map<String,Object> stats = gateway.liveStats();
                    System.out.println("Barrels ativos: " + stats.get("barrels"));
                    System.out.println("Top 10 queries: " + stats.get("topQueries"));
                }
                case 4 -> { return; }
            }
        }
    }
}
