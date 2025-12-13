package pt.uc.sd.googol.gateway; //

import java.io.Serializable;

public class Stats implements Serializable {

    private String serverName;
    private long serverUptime;
    private double avgResponseTime;
    private int indexedUrls;
    private int indexedWords;

    // Construtor vazio (boas práticas para serialização)
    public Stats() {}

    public Stats(String serverName, long serverUptime, int indexedUrls, int indexedWords) {
        this.serverName = serverName;
        this.serverUptime = serverUptime;
        this.indexedUrls = indexedUrls;
        this.indexedWords = indexedWords;
        this.avgResponseTime = 0.0;
    }

    // --- GETTERS E SETTERS ---
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public long getServerUptime() { return serverUptime; }
    public void setServerUptime(long serverUptime) { this.serverUptime = serverUptime; }

    public int getIndexedUrls() { return indexedUrls; }
    public void setIndexedUrls(int indexedUrls) { this.indexedUrls = indexedUrls; }

    public int getIndexedWords() { return indexedWords; }
    public void setIndexedWords(int indexedWords) { this.indexedWords = indexedWords; }

    public double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
}