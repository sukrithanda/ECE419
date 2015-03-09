import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

public class NameServer {
    public static void main (String[] args) throws IOException {
        ServerSocket nameServerS = null;
        /* Create Lookup server socket */
        try { 
            if (args.length == 1) {
                nameServerS = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: 0 or more than 1 arguments");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.exit(-1);
        }

        /* Store lookup table into a hashmap max 4 for now as we are only supporting 4 players*/
        ConcurrentHashMap<Integer, DataPacket> clientHash = new ConcurrentHashMap<Integer, DataPacket>(4);


        /* Listen for clients */
        while (true) {
            new NameServerHandler(nameServerS.accept(), clientHash).start();
        }
    }
}



