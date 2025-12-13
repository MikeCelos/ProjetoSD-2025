package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.multicast.ReliableMulticast;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import pt.uc.sd.googol.queue.URLQueueInterface;

/**
 * Componente principal do Web Crawler (Downloader).
 * <p>
 * Responsável por gerir um conjunto de threads (workers) que descarregam e processam páginas Web.
 * Liga-se à {@link URLQueueInterface} remota para obter URLs e reportar a sua presença,
 * e utiliza o protocolo {@link ReliableMulticast} para enviar os dados processados para os Barrels.
 *
 * @author André Ramos 2023227306
 */
public class Downloader {
    
    private final int numWorkers;
    private URLQueueInterface urlQueue; 
    private final RobotsTxtParser robotsParser;
    private final ReliableMulticast multicast;
    private ExecutorService executorService;
    private List<DownloaderWorker> workers;
    
    /**
     * Construtor do Downloader.
     * Estabelece as ligações RMI com a Queue e os Barrels, e regista este Downloader
     * como ativo na Queue central.
     *
     * @param numWorkers Número de threads de crawling a iniciar.
     * @param barrelHost Host onde estão os Barrels.
     * @param barrelPort Porta do RMI Registry dos Barrels.
     * @param numBarrels Número de Barrels esperados na rede.
     * @param queueHost Host onde está a Queue.
     * @param queuePort Porta do RMI Registry da Queue.
     */
    public Downloader(int numWorkers, String barrelHost, int barrelPort, 
                      int numBarrels, String queueHost, int queuePort) {
        this.numWorkers = numWorkers;
        this.robotsParser = new RobotsTxtParser("Googol Bot 1.0");
        this.workers = new ArrayList<>();
        
        // --- PARTE 1: CONECTAR À QUEUE REMOTA ---
        try {
            System.out.println(" Conectando à URL Queue em " + queueHost + ":" + queuePort + "...");
            Registry queueRegistry = LocateRegistry.getRegistry(queueHost, queuePort);
            
            // Lookup: Procura o objeto "queue" no servidor remoto
            this.urlQueue = (URLQueueInterface) queueRegistry.lookup("queue");

            // Regista a presença deste downloader para estatísticas em tempo real
            this.urlQueue.registerDownloader();
            
            // Teste: Ping para ver se está vivo
            System.out.println( this.urlQueue.ping());

            // Shutdown Hook: Garante que o downloader se "desregistra" se for fechado (Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("A sair....");
                    urlQueue.unregisterDownloader();
                } catch (Exception e) {
                    // Ignorar erros no fecho
                }
            }));
            
        } catch (Exception e) {
            System.err.println(" ERRO CRÍTICO: Não foi possível conectar à URL Queue!");
            e.printStackTrace();
            throw new RuntimeException("Queue não encontrada. O QueueServer está a correr?");
        }

        // --- PARTE 2: CONECTAR AOS BARRELS (MULTICAST) ---
        ReliableMulticast tempMulticast = null;
        try {
            System.out.println(" Conectando a " + numBarrels + " barrels em " + barrelHost + ":" + barrelPort);
            Registry barrelRegistry = LocateRegistry.getRegistry(barrelHost, barrelPort);
            
            List<BarrelInterface> barrels = new ArrayList<>();
            
            for (int i = 0; i < numBarrels; i++) {
                try {
                    String barrelName = "barrel" + i;
                    BarrelInterface barrel = (BarrelInterface) barrelRegistry.lookup(barrelName);
                    barrel.ping(); 
                    barrels.add(barrel);
                    System.out.println( barrelName + " conectado");
                } catch (Exception e) {
                    System.err.println(" Falha ao conectar " + "barrel" + i);
                }
            }
            
            if (barrels.isEmpty()) {
                System.err.println("AVISO: Nenhum barrel encontrado. O sistema não guardará dados.");
            } else {
                tempMulticast = new ReliableMulticast(barrels);
                System.out.println(" Multicast iniciado com " + barrels.size() + " barrels");
            }
            
        } catch (Exception e) {
            System.err.println(" Erro no setup dos Barrels: " + e.getMessage());
        }
        
        this.multicast = tempMulticast;
    }
    
    /**
     * Inicia o pool de threads e distribui o trabalho pelos Workers.
     */
    public void start() {
        System.out.println("Iniciando " + numWorkers + " workers...");
        executorService = Executors.newFixedThreadPool(numWorkers);
        
        for (int i = 0; i < numWorkers; i++) {
            // Passamos a referência remota da queue para os workers
            DownloaderWorker worker = new DownloaderWorker(i, urlQueue, robotsParser, multicast);
            workers.add(worker);
            executorService.submit(worker);
        }
    }
    
    /**
     * Encerra graciosamente todos os workers e ligações.
     */
    public void shutdown() {
        System.out.println("Encerrando downloaders...");
        if (multicast != null) {
            System.out.println("Barrels ativos: " + multicast.getBarrelCount());
        }
        
        for (DownloaderWorker worker : workers) {
            worker.stop();
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        System.out.println("Fim.");
    }
    
    /**
     * Ponto de entrada da aplicação Downloader.
     *
     * @param args Argumentos da linha de comando (não utilizados na configuração atual).
     */
    public static void main(String[] args) {
        // Configurações
        int numWorkers = 3;
        int numBarrels = 2;
        
        String barrelHost = "localhost";
        int barrelPort = 1099;
        
        String queueHost = "localhost";
        int queuePort = 1098; // Porta diferente para a Queue
        
        System.out.println("=== INICIANDO DOWNLOADER ===");
        
        // Agora passamos TODOS os argumentos necessários
        Downloader downloader = new Downloader(
            numWorkers, 
            barrelHost, barrelPort, 
            numBarrels, 
            queueHost, queuePort
        );
        
        downloader.start();
        
        // Opcional: Adicionar URL inicial (seed) para começar o trabalho
        try {
            System.out.println("Enviando seed URL...");
            // Nota: Se quiseres adicionar manualmente aqui, precisarias de expor o urlQueue.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}