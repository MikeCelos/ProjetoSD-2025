package pt.uc.sd.googol.gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface StatsListener extends Remote {
    void onStatsUpdated(Map<String, Object> stats) throws RemoteException;
}