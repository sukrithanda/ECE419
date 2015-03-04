import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;


/* 
   Lookup / Naming service
   Stores key associated with all clients

   Hastable format
   key <String> : value<DataPacket>
 */
public class NameServer {
    public static void main (String[] args) throws IOException {
        ServerSocket lookupSocket = null;
        /* Create Lookup server socket */
        try { 
            if (args.length == 1) {
                lookupSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: 0 or more than 1 arguments");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Store lookup table into a hashmap */
        ConcurrentHashMap<Integer, DataPacket> lookup = new ConcurrentHashMap<Integer, DataPacket>(4);


        /* Listen for clients */
        while (true) {
            new NameServerHandler(lookupSocket.accept(), lookup).start();
           // lookupSocket.close();
        }
    }
}



