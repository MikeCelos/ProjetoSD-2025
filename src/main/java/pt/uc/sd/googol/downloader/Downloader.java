// ...existing code...
package pt.uc.sd.googol.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import pt.uc.sd.googol.barrel.BarrelInterface;

public class Downloader extends UnicastRemoteObject implements DownloaderInterface {
    private final int numWorkers;
    private final URLQueue urlQueue;
    private final RobotsTxtParser robotsParser;
    private final BarrelInterface barrel; // pode ser null se não houver barrel disponível
    private ExecutorService executorService;
    private final List<DownloaderWorker> workers;

    public Downloader(int numWorkers, String barrelHost, int barrelPort) throws RemoteException {
        super();
        this.numWorkers = numWorkers;
        this.urlQueue = new URLQueue();
        this.robotsParser = new RobotsTxtParser("Googol Bot 1.0");
        this.workers = new ArrayList<>();

        BarrelInterface found = null;
        try {
            var registry = LocateRegistry.getRegistry(barrelHost, barrelPort);
            found = (BarrelInterface) registry.lookup("barrel");
            System.out.println("[Downloader] ligado ao Barrel em " + barrelHost + ":" + barrelPort);
        } catch (Exception e) {
            System.err.println("[Downloader] aviso: não foi possível ligar ao Barrel em " + barrelHost + ":" + barrelPort + " -> " + e.getMessage());
        }
        this.barrel = found;
    }

    public void start() {
        System.out.println("Iniciando " + numWorkers + " workers...");
        executorService = Executors.newFixedThreadPool(numWorkers);
        for (int i = 0; i < numWorkers; i++) {
            DownloaderWorker worker = new DownloaderWorker(i, urlQueue, this.barrel, this.robotsParser);
            workers.add(worker);
            executorService.submit(worker);
        }
        System.out.println("Downloaders iniciados!");
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        if (url == null) return;
        String normalized = normalizeUrl(url);
        if (normalized == null || normalized.isBlank()) return;
        System.out.println("[Downloader] adicionando URL à fila: " + normalized);
        urlQueue.addURL(normalized);
    }

    public void shutdown() {
        System.out.println("Encerrando downloaders...");
        robotsParser.printStats();
        for (DownloaderWorker w : workers) w.stop();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Downloaders encerrados!");
    }

    public URLQueue getUrlQueue() {
        return urlQueue;
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.isEmpty()) return null;
        if (!u.matches("^[a-zA-Z]+://.*")) {
            u = "https://" + u;
        }
        return u;
    }

    public static void main(String[] args) {
        try {
            String barrelHost = "localhost";
            int barrelPort = 1099;
            int downloaderRegistryPort = 2100;
            int workers = 3;

            Downloader downloader = new Downloader(workers, barrelHost, barrelPort);

            try {
                LocateRegistry.createRegistry(downloaderRegistryPort);
            } catch (Exception e) {
                // registry pode já existir
            }
            Naming.rebind("//localhost:" + downloaderRegistryPort + "/downloader", downloader);
            System.out.println("[Downloader] registado em RMI //localhost:" + downloaderRegistryPort + "/downloader");

            downloader.start();

            // manter processo vivo
            synchronized (Downloader.class) {
                Downloader.class.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// ...existing code...