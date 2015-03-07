import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/*
   Lookup / Naming service thread
 */
public class NameServerHandler extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<Integer, DataPacket> table; 

    public NameServerHandler(Socket socket, ConcurrentHashMap<Integer, DataPacket> table) {
        this.socket = socket;
        NameServerHandler.table = table;
    }

    public void run() {
        try {
            ObjectInputStream server_in = new ObjectInputStream(socket.getInputStream());
            DataPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream server_out = new ObjectOutputStream(socket.getOutputStream());

            while ( (packetFromClient = (DataPacket) server_in.readObject()) != null) {
                /* create a packet to send reply back to client */
                DataPacket packetToClient = new DataPacket();

                /* LOOKUP_REGISTER */
                // Client wants to find a corresponding IP and port for a given client name
                // Assume all error handling is done in client side
                if(packetFromClient.packet_type == DataPacket.LOOKUP_REGISTER) {
                    System.out.println("From Client: LOOKUP_REGISTER ");

                    packetToClient.packet_type = DataPacket.LOOKUP_REGISTER;
                    String ip = packetFromClient.client_host;
                    int port  = packetFromClient.client_port;

                    System.out.println("Lookup is registering new client.");
                    System.out.println("IP: " + ip + " Port: " + port);

                    // Check if a client is already listening in that port.
                  //  Object[] keys = table.keySet().toArray();
                  //  int size = table.size();
                    
                    boolean portExists = false;
                    for(Entry<Integer, DataPacket> entry: table.entrySet()) {
                    	if(table.get(Integer.valueOf(entry.getKey().toString())).client_port == port){
                    		System.out.println("ERROR: port already in use ");
                    		packetToClient.error_code = DataPacket.ERROR_LOOKUP_PORT;
                    		server_out.writeObject(packetToClient);
                    		portExists = true;
                    		break;
                    	}
                       // System.out.println(entry.getKey());
                       // System.out.println(entry.getValue());
                    }
		    
                   /* for(int i = 0; i < 4; i++){                    	
                    	if(table.get(Integer.valueOf(keys[i].toString())).client_port == port){
                    		System.out.println("Two client's can't have the same port!");
                    		packetToClient.error_code = MazePacket.ERROR_LOOKUP_PORT;
                    		server_out.writeObject(packetToClient);
                    		portExists = true;
                    		break;
                    	}
                    }*/
		    
                    if(portExists){
                    	continue;
                    }
	
                 
                    DataPacket newClient = new DataPacket();		    
                    newClient = packetFromClient;
            
                    // Find empty ID
                    int client_id = 1;
                    while(table.putIfAbsent(client_id, newClient) != null){
                        client_id++;
                    }
                    System.out.print("ID is... "  + client_id);

          
                    System.out.println("To Client: registration  success ");
                    packetToClient.client_id = client_id;
                    packetToClient.error_code = 0;

                    packetToClient.NameServerTable = new ConcurrentHashMap<Integer, DataPacket>();
                    packetToClient.NameServerTable = table;

                    server_out.writeObject(packetToClient);
                    continue;
                }


                /* CLIENT_QUIT */
                if (packetFromClient.packet_type == DataPacket.LOOKUP_QUIT) {
                    packetToClient.packet_type = DataPacket.LOOKUP_REPLY;

                    System.out.println("From Client: CLIENT_QUIT ");

                    table.remove(packetFromClient.client_id);

                    System.out.println("To Client: Removed from naming service.");
                    server_out.writeObject(packetToClient);
                    continue;
                }

                /* if code comes here, there is an error in the packet */
                System.err.println("ERROR: Unknown packet!!");
                System.exit(-1);

                // DEBUGGING!
                continue;
            }

            /* cleanup when client exits */
            server_in.close();
            server_out.close();
            socket.close();

        } catch (IOException e) {
                e.printStackTrace();
        } catch (ClassNotFoundException e) {
                e.printStackTrace();
        }
    }

}
