package pt.uc.sd.googol.queue;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI que define as operações remotas disponíveis no servidor de Fila (URL Queue).
 * <p>
 * Esta interface estabelece o contrato de comunicação entre:
 * <ul>
 * <li><b>Downloaders:</b> Que consomem URLs para processar e reportam novos links encontrados.</li>
 * <li><b>Gateway:</b> Que insere URLs prioritários pedidos pelos clientes e consulta estatísticas.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
public interface URLQueueInterface extends Remote {

    /**
     * Adiciona um URL ao final da fila de processamento.
     * Utilizado pelos Downloaders quando encontram novos links numa página.
     *
     * @param url O URL a adicionar.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addURL(String url) throws RemoteException;

    /**
     * Adiciona um URL ao início da fila (Prioridade Máxima).
     * Utilizado pelo Gateway quando um utilizador pede a indexação manual de um site.
     *
     * @param url O URL a processar com urgência.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addTopPriorityURL(String url) throws RemoteException;

    /**
     * Obtém o próximo URL da fila para ser processado.
     *
     * @return O URL a visitar, ou null se a fila estiver vazia/timeout.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    String getNextURL() throws RemoteException;

    /**
     * Marca um URL como visitado na lista de controlo.
     * Impede que o mesmo URL seja adicionado novamente à fila no futuro.
     *
     * @param url O URL que foi processado.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void markAsVisited(String url) throws RemoteException;

    /**
     * Obtém o número de URLs atualmente à espera na fila.
     *
     * @return Tamanho da fila.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    int getQueueSize() throws RemoteException;

    /**
     * Obtém o número total de URLs que já foram processados desde o início da execução.
     *
     * @return Contagem de URLs visitados.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    int getVisitedCount() throws RemoteException;

    /**
     * Verifica a conectividade com o servidor da Queue.
     *
     * @return Mensagem de estado.
     * @throws RemoteException Se o servidor estiver incontactável.
     */
    String ping() throws RemoteException;

    /**
     * Regista a entrada de um novo Downloader (worker) no sistema.
     * Incrementa o contador de workers ativos.
     *
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void registerDownloader() throws RemoteException;

    /**
     * Regista a saída de um Downloader do sistema.
     * Decrementa o contador de workers ativos.
     *
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void unregisterDownloader() throws RemoteException;

    /**
     * Obtém o número de Downloaders atualmente ativos e conectados à Queue.
     *
     * @return O número de workers a trabalhar.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    int getActiveDownloaders() throws RemoteException;
}