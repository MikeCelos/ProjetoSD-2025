package pt.uc.sd.googol.downloader;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DownloaderInterface extends Remote {
    void addURL(String url) throws RemoteException;
}
