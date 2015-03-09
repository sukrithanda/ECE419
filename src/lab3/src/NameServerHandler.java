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
    private Socket s = null;
    private static ConcurrentHashMap<Integer, DataPacket> clientHash; 

    public NameServerHandler(Socket socket, ConcurrentHashMap<Integer, DataPacket> clientHash) {
        this.s = socket;
        NameServerHandler.clientHash = clientHash;
    }

    public void run() {
        try {
            ObjectInputStream server_in = new ObjectInputStream(s.getInputStream());
            DataPacket packetIncoming;

            /* stream to write back to client */
            ObjectOutputStream server_out = new ObjectOutputStream(s.getOutputStream());

            try {
				while ( (packetIncoming = (DataPacket) server_in.readObject()) != null) {
				    DataPacket packetOutgoing = new DataPacket();
				    
				    if(packetIncoming.packet_type == DataPacket.LOOKUP_REGISTER) {
				        System.out.println("From Client: LOOKUP_REGISTER ");

				        packetOutgoing.packet_type = DataPacket.LOOKUP_REGISTER;
				        
				        System.out.println("NameServer is registering new client.");
				        System.out.println("IP: " + packetIncoming.client_host + " Port: " + packetIncoming.client_port);

				        
				        int portinuse = 0;
				        for(Entry<Integer, DataPacket> entry: clientHash.entrySet()) {
				        	if(clientHash.get(Integer.valueOf(entry.getKey().toString())).client_port == packetIncoming.client_port){
				        		System.out.println("ERROR: port already in use ");
				        		packetOutgoing.error_code = DataPacket.ERROR_LOOKUP_PORT;
				        		server_out.writeObject(packetOutgoing);
				        		portinuse = 1;
				        		break;
				        	}
				 
				        }
				
				        if(portinuse == 1){
				        	continue;
				        }

				     
				        DataPacket newClient = new DataPacket();		    
				        newClient = packetIncoming;
				
				        // Find empty ID
				        int newID = 1;
				        while(clientHash.putIfAbsent(newID, newClient) != null){
				            newID++;
				        }
				        System.out.print("New client ID is "  + newID);

        
				        System.out.println("To Client: registration  success ");
				        packetOutgoing.client_id = newID;
				        packetOutgoing.error_code = 0;

				        packetOutgoing.NameServerTable = new ConcurrentHashMap<Integer, DataPacket>();
				        packetOutgoing.NameServerTable = clientHash;

				        server_out.writeObject(packetOutgoing);
				        continue;
				    }


				    /* CLIENT_QUIT */
				    if (packetIncoming.packet_type == DataPacket.LOOKUP_QUIT) {
				        packetOutgoing.packet_type = DataPacket.LOOKUP_REPLY;

				        System.out.println("From Client: CLIENT_QUIT ");

				        clientHash.remove(packetIncoming.client_id);

				        System.out.println("To Client: Removed from naming service.");
				        server_out.writeObject(packetOutgoing);
				        continue;
				    }

				    /* if code comes here, there is an error in the packet */
				    System.err.println("Packet not recognized exiting!");
				    System.exit(-1);

				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            /* cleanup when client exits */
            server_in.close();
            server_out.close();
            s.close();

        } catch (IOException e) {
                e.printStackTrace();
        }
    }

}
