/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1
 *  Componente: Gateway
 *  Classe: Stats
 * ===============================================================
 *
 * <p>
 * A classe {@code Stats} representa um conjunto de <b>estatísticas
 * de funcionamento</b> de um componente do sistema (por exemplo,
 * Gateway ou Barrel).
 * </p>
 *
 * <p>
 * É utilizada para transportar métricas de monitorização entre
 * componentes distribuídos, permitindo ao Gateway recolher e
 * apresentar informação sobre o estado do sistema.
 * </p>
 *
 * <p>
 * As estatísticas atualmente suportadas incluem:
 * <ul>
 *   <li>Nome do servidor;</li>
 *   <li>Tempo de funcionamento (uptime);</li>
 *   <li>Tempo médio de resposta;</li>
 *   <li>Número de URLs indexados;</li>
 *   <li>Número de palavras indexadas.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Esta classe:
 * <ul>
 *   <li>Implementa {@link Serializable} para permitir transmissão
 *       de instâncias via RMI;</li>
 *   <li>Funciona como um <i>DTO</i> (Data Transfer Object), não
 *       contendo lógica de negócio;</li>
 *   <li>É usada para fins de monitorização, diagnóstico e
 *       apresentação de estatísticas ao utilizador.</li>
 * </ul>
 * </p>
 *
 * @author
 *  Elemento 1: Francisco Vasconcelos e Sá Pires da Silva
 *  Elemento 2: André Ramos
 */

package pt.uc.sd.googol.gateway; 

import java.io.Serializable;

public class Stats implements Serializable {

    private String serverName;
    private long serverUptime;
    private double avgResponseTime;
    private int indexedUrls;
    private int indexedWords;

    // 1. CONSTRUTOR VAZIO (Obrigatório para 'new Stats()')
    public Stats() {}

    // 2. CONSTRUTOR COMPLETO
    public Stats(String serverName, long serverUptime, int indexedUrls, int indexedWords) {
        this.serverName = serverName;
        this.serverUptime = serverUptime;
        this.indexedUrls = indexedUrls;
        this.indexedWords = indexedWords;
        this.avgResponseTime = 0.0;
    }

    // Construtor de conveniência para quando não temos o Uptime (assume 0)
    public Stats(String serverName, int indexedUrls, int indexedWords) {
        this.serverName = serverName;
        this.serverUptime = 0L; // Assume 0 por defeito
        this.indexedUrls = indexedUrls;
        this.indexedWords = indexedWords;
        this.avgResponseTime = 0.0;
    }


    // ... (getters e setters) ...

    // 3. SETTERS (Obrigatórios para 'setAvgResponseTime', etc.)
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public long getServerUptime() { return serverUptime; }
    public void setServerUptime(long serverUptime) { this.serverUptime = serverUptime; }

    public double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

    public int getIndexedUrls() { return indexedUrls; }
    public void setIndexedUrls(int indexedUrls) { this.indexedUrls = indexedUrls; }

    public int getIndexedWords() { return indexedWords; }
    public void setIndexedWords(int indexedWords) { this.indexedWords = indexedWords; }
}
