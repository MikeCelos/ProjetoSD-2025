package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.common.SearchResult;

public interface BarrelInterface extends Remote {
    // Escrita do índice (pelos Downloaders)
    void addDocument(PageInfo page) throws RemoteException;         // indexa um URL (tokens, título, links)
    void addBacklinks(String url, List<String> incoming) throws RemoteException;

    // Leitura (pela Gateway)
    List<SearchResult> searchAllTerms(List<String> terms, int page, int pageSize) throws RemoteException;
    List<String> getBacklinks(String url) throws RemoteException;

    // Gestão / introspecção
    String ping() throws RemoteException;
    Map<String, Object> stats() throws RemoteException;             // tamanho do índice, média latência, etc.
}
