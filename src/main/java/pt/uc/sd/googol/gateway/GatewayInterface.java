package pt.uc.sd.googol.gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import pt.uc.sd.googol.common.SearchResult;

public interface GatewayInterface extends Remote {
    List<SearchResult> search(String query) throws RemoteException; // <- TEM de existir assim
    String addURL(String url) throws RemoteException;
}