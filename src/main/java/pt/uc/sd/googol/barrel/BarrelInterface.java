package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;

/**
 * Interface RMI que define as operações remotas disponíveis nos Storage Barrels.
 * <p>
 * Esta interface é o contrato para a comunicação entre os componentes do sistema
 * (Downloaders, Gateway e outros Barrels) e a camada de armazenamento.
 * Permite a indexação de documentos, pesquisa, consulta de estatísticas e
 * sincronização de estado para tolerância a falhas.
 *
 * @author André Ramos 2023227306
 */
public interface BarrelInterface extends Remote {

    /**
     * Adiciona uma página processada ao índice do Barrel.
     * Este método é invocado pelos Downloaders através do protocolo de Multicast RMI
     * para garantir que a informação é replicada.
     *
     * @param page Objeto {@link PageInfo} contendo o URL, título, citação e palavras extraídas da página.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addDocument(PageInfo page) throws RemoteException;
    
    /**
     * Realiza uma pesquisa no índice invertido por um conjunto de termos.
     *
     * @param terms Lista de palavras-chave a pesquisar.
     * @param page Número da página de resultados para paginação (0 para a primeira página).
     * @return Lista de objetos {@link SearchResult} contendo os resultados encontrados, ordenados por relevância.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    List<SearchResult> search(List<String> terms, int page) throws RemoteException;
    
    /**
     * Obtém a lista de URLs que contêm hiperligações para o URL especificado.
     *
     * @param url O URL de destino para o qual se pretendem encontrar referências (backlinks).
     * @return Lista de Strings com os URLs das páginas que apontam para o URL alvo.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    List<String> getBacklinks(String url) throws RemoteException;
    
    /**
     * Retorna uma string formatada com as estatísticas atuais do Barrel.
     * Inclui o número total de páginas indexadas, termos únicos e backlinks registados.
     *
     * @return String informativa com o estado interno do Barrel.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    String getStats() throws RemoteException;
    
    /**
     * Método de verificação de disponibilidade (heartbeat).
     * Usado pelo Gateway e Downloaders para confirmar se este Barrel está ativo e responsivo.
     *
     * @return Uma mensagem de confirmação (ex: "PONG").
     * @throws RemoteException Se o Barrel estiver incontactável.
     */
    String ping() throws RemoteException;
    
    /**
     * Obtém o estado completo dos dados do Barrel para fins de sincronização.
     * Este método é crítico para a tolerância a falhas, permitindo que um Barrel que reinicie
     * copie os dados de um Barrel vizinho que já esteja em funcionamento (State Transfer).
     *
     * @return Objeto {@link SyncData} contendo todos os mapas de dados (páginas, índice, backlinks).
     * @throws RemoteException Se ocorrer um erro na transferência dos dados.
     */
    SyncData getFullState() throws RemoteException;
}