/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1
 *  Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 *  Ficheiro: BarrelInterface.java
 * ===============================================================
 *
 *  @Resumo:
 *  Interface RMI que define o contrato remoto entre o Gateway,
 *  o Downloader e os Barrels. É a camada RPC responsável por
 *  expor os métodos de indexação, pesquisa e monitorização do
 *  sistema distribuído.
 *
 *  @Arquitetura:
 *  O Barrel é um servidor RMI registado no Registry sob o nome
 *  "barrel<ID>". Através desta interface, outros módulos do
 *  sistema (Downloader e Gateway) podem comunicar com ele.
 *
 *  @Métodos:
 *   - addDocument(PageInfo page):
 *       Recebe uma página completa (URL, título, palavras, links)
 *       e adiciona-a ao índice invertido e ao mapa de backlinks.
 *
 *   - search(List<String> terms, int page):
 *       Executa a pesquisa de interseção de termos e devolve
 *       resultados paginados (10 por página) ordenados por
 *       relevância (número de backlinks).
 *
 *   - getBacklinks(String url):
 *       Devolve as páginas que referenciam a URL fornecida.
 *
 *   - getStats():
 *       Informa o número de páginas indexadas, termos e backlinks.
 *
 *   - ping():
 *       Utilizado para heartbeat e deteção de falhas pelo Gateway.
 *
 *  @RMI e Failover:
 *  Cada método pode lançar RemoteException, permitindo ao Gateway
 *  detetar falhas e redirecionar as chamadas para outro Barrel
 *  (failover transparente).
 *
 *  @Replicação:
 *  Esta interface suporta múltiplas instâncias de Barrel no sistema,
 *  facilitando replicação e balanceamento de carga.
 */

package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;

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