package search;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;

public class IndexServer extends UnicastRemoteObject implements Index {
    //private String[] urlsToIndex;
    private List<String> urlsToIndex = new ArrayList<String>();
    private ConcurrentHashMap<String, List<String>> index = new ConcurrentHashMap<>();

    private RobotInterface robot;

    public IndexServer() throws RemoteException {
        super();
        //This structure has a number of problems. The first is that it is fixed size. Can you enumerate the others?
               
    }

    public static void main(String args[]) {
        try {
            IndexServer server = new IndexServer(); //server object
            Registry registry = LocateRegistry.createRegistry(8183); //registo de RMI
            registry.rebind("index", server); //usa o regito para registar o objecto do server
            System.out.println("Server ready. Waiting for input...");

            //TODO: This approach needs to become interactive. Use a Scanner(System.in) to create a rudimentary user interface to:
            //1. Add urls for indexing
            //2. search indexed urls
            //server.putNew("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal");
            Scanner keyboard = new Scanner(System.in);
            String line;
            while((line = keyboard.nextLine()) != "")
                if(line.startsWith("http:") || line.startsWith("https:"))
                    server.putNew(line);
                else
                    server.searchWord(line).forEach(System.out::println);

            keyboard.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private long counter = 0, timestamp = System.currentTimeMillis();;

    public String takeNext() throws RemoteException {
        //TODO: not implemented fully. Prefer structures that return in a push/pop fashion

        String nextUrl = "";
        if(urlsToIndex.size()>0){
            nextUrl = urlsToIndex.remove(0);
        }

        return nextUrl;
    }

    public void putNew(String url) throws java.rmi.RemoteException {
        //TODO: Example code. Must be changed to use structures that have primitives such as .add(...)
        urlsToIndex.add(url);

    }

    public void addToIndex(String word, String url) throws java.rmi.RemoteException {
        //TODO: not implemented
        //System.out.println("Indexing " + word + " for " + url);
        if(index.containsKey(word)){
            //System.out.println("Already indexed " + word + ", adding new url");
            ArrayList<String> palavrasParaUrls = (ArrayList<String>)index.get(word);
            palavrasParaUrls.add(url);
            index.put(word, palavrasParaUrls);
        }else {
            //System.out.println("New word, adding to index");
            ArrayList<String> palavrasNovas = new ArrayList<String>();
            palavrasNovas.add(url);
            index.put(word, palavrasNovas);
        }
    }

    
    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        //TODO: not implemented
        System.out.println("Searching for " + word);
        robot.printOnWorker("a");
        if(index.containsKey(word)){
            System.out.println("Found " + index.get(word).size() + " results");
            ArrayList<String> resultadoPesquisa = (ArrayList<String>)index.get(word);
            return resultadoPesquisa;
        }
        return new ArrayList<String>();
    }

    public void subscribeRobot(RobotInterface robot) throws java.rmi.RemoteException {
        this.robot = robot;
    }
}
