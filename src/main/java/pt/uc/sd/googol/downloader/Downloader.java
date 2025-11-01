package pt.uc.sd.googol.downloader;

import pt.uc.sd.googol.barrel.BarrelInterface;
import pt.uc.sd.googol.downloader.DownloaderWorker;
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
    private final BarrelInterface barrel; // ← ADICIONAR
    private ExecutorService executorService;
    private List<DownloaderWorker> workers;
    
    public Downloader(int numWorkers, String barrelHost, int barrelPort) {
        this.numWorkers = numWorkers;
        this.urlQueue = new URLQueue();
        this.robotsParser = new RobotsTxtParser("Googol Bot 1.0");
        this.workers = new ArrayList<>();
        
        // ← CONECTAR AO BARREL
        BarrelInterface tempBarrel = null;
        try {
            System.out.println("🔍 Conectando ao Barrel em " + barrelHost + ":" + barrelPort);
            Registry registry = LocateRegistry.getRegistry(barrelHost, barrelPort);
            tempBarrel = (BarrelInterface) registry.lookup("barrel");
            
            // Testar conexão
            String pong = tempBarrel.ping();
            System.out.println("✓ Barrel conectado: " + pong);
            
        } catch (Exception e) {
            System.err.println("⚠ Aviso: Barrel não disponível, rodando sem RMI");
            System.err.println("  Erro: " + e.getMessage());
        }
        this.barrel = tempBarrel;
    }
    
    public void start() {
        System.out.println("Iniciando " + numWorkers + " workers...");
        executorService = Executors.newFixedThreadPool(numWorkers);
        
        for (int i = 0; i < numWorkers; i++) {
            DownloaderWorker worker = new DownloaderWorker(i, urlQueue, robotsParser, barrel);
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
        // Downloader COM RMI
        Downloader downloader = new Downloader(3, "localhost", 1099);
        
        downloader.start();
        
        // URLs de teste
        downloader.addURL("https://www.uc.pt");
        downloader.addURL("https://www.dei.uc.pt");
        downloader.addURL("https://en.wikipedia.org/wiki/Web_crawler");
        
        try {
            Thread.sleep(30000); // 30 segundos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        downloader.shutdown();
    }
}