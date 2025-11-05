package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.multicast.ReliableMulticast;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Downloader {
    private final int numWorkers;
    private final URLQueue urlQueue;
    private final RobotsTxtParser robotsParser;
    private final ReliableMulticast multicast; // ← MUDOU
    private ExecutorService executorService;
    private List<DownloaderWorker> workers;
    
    public Downloader(int numWorkers, String barrelHost, int barrelPort, int numBarrels) {
        this.numWorkers = numWorkers;
        this.urlQueue = new URLQueue();
        this.robotsParser = new RobotsTxtParser("Googol Bot 1.0");
        this.workers = new ArrayList<>();
        
        // ← CONECTAR A MÚLTIPLOS BARRELS
        ReliableMulticast tempMulticast = null;
        try {
            System.out.println(" Conectando a " + numBarrels + " barrels em " + barrelHost + ":" + barrelPort);
            Registry registry = LocateRegistry.getRegistry(barrelHost, barrelPort);
            
            List<BarrelInterface> barrels = new ArrayList<>();
            
            for (int i = 0; i < numBarrels; i++) {
                try {
                    String barrelName = "barrel" + i;
                    BarrelInterface barrel = (BarrelInterface) registry.lookup(barrelName);
                    
                    // Testar conexão
                    String pong = barrel.ping();
                    barrels.add(barrel);
                    System.out.println(" " + barrelName + " conectado: " + pong);
                    
                } catch (Exception e) {
                    System.err.println(" Não foi possível conectar ao barrel" + i + ": " + e.getMessage());
                }
            }
            
            if (barrels.isEmpty()) {
                throw new Exception("Nenhum barrel disponível!");
            }
            
            // Criar multicast com os barrels disponíveis
            tempMulticast = new ReliableMulticast(barrels);
            System.out.println("✓ Reliable Multicast inicializado com " + barrels.size() + " barrels");
            
        } catch (Exception e) {
            System.err.println(" Aviso: Barrels não disponíveis, rodando sem RMI");
            System.err.println("  Erro: " + e.getMessage());
        }
        
        this.multicast = tempMulticast;
    }
    
    public void start() {
        System.out.println("Iniciando " + numWorkers + " workers...");
        executorService = Executors.newFixedThreadPool(numWorkers);
        
        for (int i = 0; i < numWorkers; i++) {
            DownloaderWorker worker = new DownloaderWorker(i, urlQueue, robotsParser, multicast);
            workers.add(worker);
            executorService.submit(worker);
        }
        
        System.out.println("Downloaders iniciados!");
    }
    
    public void addURL(String url) {
        urlQueue.addURL(url);
    }
    
    public void shutdown() {
        System.out.println("Encerrando downloaders...");
        robotsParser.printStats();
        
        if (multicast != null) {
            System.out.println("Barrels ativos: " + multicast.getBarrelCount());
        }
        
        for (DownloaderWorker worker : workers) {
            worker.stop();
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        System.out.println("Downloaders encerrados!");
    }
    
    public URLQueue getUrlQueue() {
        return urlQueue;
    }
    
    public static void main(String[] args) {
        int numBarrels = 2; // ← CONFIGURAR: número de barrels
        
        // Downloader COM MULTICAST para 2 barrels
        Downloader downloader = new Downloader(3, "localhost", 1099, numBarrels);
        
        downloader.start();
        
        // URLs de teste
        downloader.addURL("https://www.uc.pt");
        downloader.addURL("https://www.dei.uc.pt");
        downloader.addURL("https://en.wikipedia.org/wiki/Web_crawler");
        
        try {
            Thread.sleep(60000); // 60 segundos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        downloader.shutdown();
    }
}