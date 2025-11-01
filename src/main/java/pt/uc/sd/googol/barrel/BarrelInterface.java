package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import pt.uc.sd.googol.common.PageInfo;

public interface BarrelInterface extends Remote {
    void addDocument(PageInfo page) throws RemoteException;
    String ping() throws RemoteException;
}
