package search;

import java.rmi.*;

public interface RobotInterface extends Remote{
	public void printOnWorker(String toPrint) throws java.rmi.RemoteException;
}
