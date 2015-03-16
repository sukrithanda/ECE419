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

    int test = 1;
    
    public void print(String str) {
        if (test == 1) {
            System.out.println("DEBUG: (NameServerHandler) " + str);
        }
    }
    
    public void run() {
        try {
            ObjectInputStream server_in = new ObjectInputStream(s.getInputStream());
            DataPacket packetIncoming;
            ObjectOutputStream server_out = new ObjectOutputStream(s.getOutputStream());

            try {
				while ( (packetIncoming = (DataPacket) server_in.readObject()) != null) {
				    DataPacket packetOutgoing = new DataPacket();
				    
				    if(packetIncoming.scenarioType == DataPacket.NS_REGISTER) {
				        print("Player request to register on NameServer");

				        packetOutgoing.scenarioType = DataPacket.NS_REGISTER;
				        
				        print("New Player registered on NameServer");
				        print("Port: " + packetIncoming.portNum + "; IP Address: " + packetIncoming.hostName);

				        
				        int portinuse = 0;
				        for(Entry<Integer, DataPacket> entry: clientHash.entrySet()) {
				        	if(clientHash.get(Integer.valueOf(entry.getKey().toString())).portNum == packetIncoming.portNum){
				        		print("Port is not available " + packetIncoming.portNum);
				        		packetOutgoing.errType = DataPacket.ERR_RESERVED_NS_PORT;
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
				        
				        print("New player's ID is "  + newID);
				        print("NameServer informing player of successful registration");
				        
				        packetOutgoing.playerID = newID;
				        packetOutgoing.errType = 0;

				        packetOutgoing.NSTable = new ConcurrentHashMap<Integer, DataPacket>();
				        packetOutgoing.NSTable = clientHash;

				        server_out.writeObject(packetOutgoing);
				        continue;
				    }
				    
				    if (packetIncoming.scenarioType == DataPacket.NS_QUIT) {
				        packetOutgoing.scenarioType = DataPacket.NS_RESPONSE;

				        print("Player request to quit");

				        clientHash.remove(packetIncoming.playerID);

				        print("Player deleted from NameServer");
				        server_out.writeObject(packetOutgoing);
				        continue;
				    }

				    //error in packet
				    System.err.println("Packet not recognized. Exiting!");
				    System.exit(-1);

				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

           //client exit
            server_in.close();
            server_out.close();
            s.close();

        } catch (IOException e) {
                e.printStackTrace();
        }
    }

}
