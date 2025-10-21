package search;

import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Robot extends UnicastRemoteObject implements RobotInterface {

    public Robot() throws java.rmi.RemoteException {
        super();
    }

    public void printOnWorker(String toPrint) throws java.rmi.RemoteException {
        System.out.println(toPrint);
    }

    public static void main(String[] args) {
        try {
            Robot robot = new Robot();
            Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index");
            index.subscribeRobot(robot);
            while (true) {
                String url = index.takeNext();
                if(url==""){
                    try {
                        System.out.println("No URLs, sleeping...");
                        Thread.sleep(1000);
                        continue;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                System.out.println(url);
                Document doc = Jsoup.connect(url).get();

                StringTokenizer tokens = new StringTokenizer(doc.text());

                int countTokens = 0;
                while (tokens.hasMoreElements()){
                    String novaPalavra = tokens.nextToken().toLowerCase();
                    index.addToIndex(novaPalavra, url);
                }

                Elements links = doc.select("a[href]");
                for (Element link : links){
                    index.putNew(link.attr("abs:href"));
                }
                
                //Todo: Read JSOUP documentation and parse the html to index the keywords. 
                //Then send back to server via index.addToIndex(...)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
