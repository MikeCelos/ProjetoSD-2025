package pt.uc.sd.googol.downloader;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface URLQueueInterface extends Remote {
    void addURL(String url) throws RemoteException;
    void addTopPriorityURL(String url) throws RemoteException;
    String getNextURL() throws RemoteException;
    void markAsVisited(String url) throws RemoteException;
    int getQueueSize() throws RemoteException;
    int getVisitedCount() throws RemoteException;
    String ping() throws RemoteException;
    void registerDownloader() throws RemoteException;
    void unregisterDownloader() throws RemoteException;
    int getActiveDownloaders() throws RemoteException;
}