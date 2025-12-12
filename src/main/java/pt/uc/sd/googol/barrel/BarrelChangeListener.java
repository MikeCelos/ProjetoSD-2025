package pt.uc.sd.googol.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BarrelChangeListener extends Remote {
    void onBarrelUpdate() throws RemoteException;
}