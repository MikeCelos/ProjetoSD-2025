package gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface Gateway extends Remote {
    // Pesquisa AND com paginação de 10 por página
    List<String> search(String query, int page) throws RemoteException;

    // Devolve estatísticas agregadas: top-10 queries e latência média por barrel (ms)
    Map<String, Object> stats() throws RemoteException;

    // Registar dinamicamente um Barrel (IndexServer) pelo URL RMI, ex: rmi://host:8183/index
    void registerBarrel(String rmiUrl) throws RemoteException;
}
