package pt.uc.sd.googol.gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GatewayInterface extends Remote {
    /**
     * Pesquisa por termos
     * @param terms Lista de termos de pesquisa
     * @param page Número da página (começando em 0)
     * @return Lista de resultados
     */
    List<SearchResult> search(List<String> terms, int page) throws RemoteException;
    
    /**
     * Obtém páginas que apontam para um URL
     * @param url URL alvo
     * @return Lista de URLs que apontam para o alvo
     */
    List<String> getBacklinks(String url) throws RemoteException;
    
    /**
     * Obtém estatísticas do sistema
     * @return Mapa com estatísticas
     */
    String getStats() throws RemoteException;
    
    /**
     * Testa se o gateway está ativo
     */
    String ping() throws RemoteException;

    boolean indexUrl(String url) throws RemoteException;

    int getQueueSize() throws RemoteException;

    public int getActiveDownloaders() throws RemoteException;
}