package pt.uc.sd.googol.gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface RMI que define o ponto de entrada (Gateway) do sistema Googol.
 * <p>
 * Esta interface é disponibilizada aos clientes (CLI e Web) e serve como fachada
 * para todo o sistema distribuído. O Gateway encaminha os pedidos para os
 * componentes apropriados (Barrels para pesquisa, Queue para indexação) e
 * agrega estatísticas do sistema.
 *
 * @author André Ramos 2023227306
 */
public interface GatewayInterface extends Remote {

    /**
     * Realiza uma pesquisa no índice do motor de busca.
     * O Gateway encaminha este pedido para um dos Barrels ativos usando balanceamento de carga.
     *
     * @param terms Lista de palavras-chave a pesquisar.
     * @param page Número da página de resultados (0 para a primeira página).
     * @return Lista de objetos {@link SearchResult} com os resultados encontrados.
     * @throws RemoteException Se ocorrer um erro na comunicação com o Gateway ou Barrels.
     */
    List<SearchResult> search(List<String> terms, int page) throws RemoteException;
    
    /**
     * Obtém a lista de páginas que contêm hiperligações para o URL especificado.
     *
     * @param url O URL de destino para o qual se pretendem encontrar referências.
     * @return Lista de Strings com os URLs das páginas que apontam para o URL alvo.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    List<String> getBacklinks(String url) throws RemoteException;
    
    /**
     * Obtém um relatório textual com as estatísticas gerais do sistema.
     * Inclui o estado dos Barrels, Top 10 de pesquisas, tempos de resposta e contagens de dados.
     *
     * @return String formatada com as estatísticas.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    String getStats() throws RemoteException;
    
    /**
     * Verifica a conectividade com o Gateway.
     *
     * @return Uma mensagem de confirmação se o serviço estiver ativo.
     * @throws RemoteException Se o Gateway estiver incontactável.
     */
    String ping() throws RemoteException;

    /**
     * Permite a um cliente submeter manualmente um URL para indexação.
     * O URL é enviado para a fila de processamento (Queue) com prioridade máxima.
     *
     * @param url O endereço Web a ser indexado.
     * @return true se o URL foi aceite e colocado na fila, false caso contrário.
     * @throws RemoteException Se ocorrer um erro na comunicação com a Queue.
     */
    boolean indexUrl(String url) throws RemoteException;

    /**
     * Consulta o tamanho atual da fila de URLs pendentes para processamento.
     *
     * @return O número de URLs à espera na Queue.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    int getQueueSize() throws RemoteException;

    /**
     * Consulta o número de Downloaders (Web Crawlers) atualmente ativos no sistema.
     *
     * @return O número de workers registados e a trabalhar.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    public int getActiveDownloaders() throws RemoteException;

    // WebServer chama isto para começar a receber atualizações
    void registerListener(StatsListener listener) throws RemoteException;

    // Barrels chamam isto para avisar que indexaram algo novo
    void barrelNotifyUpdate() throws RemoteException;
}