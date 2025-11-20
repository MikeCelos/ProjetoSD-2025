package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import pt.uc.sd.googol.common.PageInfo;
import pt.uc.sd.googol.gateway.SearchResult;

public interface BarrelInterface extends Remote {
    void addDocument(PageInfo page) throws RemoteException;
    
    List<SearchResult> search(List<String> terms, int page) throws RemoteException;
    
    List<String> getBacklinks(String url) throws RemoteException;
    
    String getStats() throws RemoteException;
    
    String ping() throws RemoteException;
    
    SyncData getFullState() throws RemoteException;
}
