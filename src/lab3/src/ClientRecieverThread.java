import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/*
 * ClientReceiverThread - Receives DataPackets from each remote client
 */

public class ClientRecieverThread extends Thread {
    DataPacket packetOut;
    DataPacket packetIn;
    
    ObjectInputStream inStream;
    ObjectOutputStream outStream;
    
    ClientHandlerThread clientHandler;
    Broadcaster broadcaster;
    int laportCLK;

    DataPacketManager packetManager = null;
    Socket remoteS = null;

    boolean kill = false;
    boolean DEBUG = true;

    public ClientRecieverThread (ClientHandlerThread clientHandler, Socket socket, 
    		DataPacketManager listener, Broadcaster broadcaster) throws IOException {
        try {
        	print("Starting");
            this.remoteS = socket;
            this.broadcaster = broadcaster;
            this.clientHandler = clientHandler;
            this.packetManager = listener;
            this.outStream = new ObjectOutputStream(remoteS.getOutputStream());
        } catch (IOException e) {
        }
    }

    public void run() {
        try {
            inStream = new ObjectInputStream(remoteS.getInputStream());
            while (((packetIn = (DataPacket) inStream.readObject()) != null) && !kill) {
                print("Received packet type: " + packetIn.packet_type);
                
                DataPacket dataPacket = new DataPacket();

                /* Process each packet */
                switch (packetIn.packet_type) {
                    case DataPacket.CLIENT_CLOCK:
                       // clientClock();
                        int requested_lc = packetIn.lamportClock;    
                        dataPacket.packet_type = DataPacket.CLIENT_ACK;
                        packetManager.acquireLock();

                        print("requested_lc: " + requested_lc + " current lamportClock: " + packetManager.getLamportClock());
                        dataPacket.lamportClock = packetManager.getLamportClock();

                        if(requested_lc >= laportCLK){
                            print("incrementing my lc after recieving CLIENT_CLOCK packet");
                            // Clock is valid!
                            if(requested_lc == 19){
                            	packetManager.setLamportClock(0);
                            } else {
                            	packetManager.setLamportClock(++requested_lc);
                            }
                            //Set up and send awknowledgement packet
                            //eventPacket.lamportClock = lamportClock;]
                            dataPacket.isValidClock = true;

                            print("Incremented lc is " + packetManager.getLamportClock());

                        } else{
                            // Oh no! The lamport clock is not valid.
                            // Send the latest lamport clock and disawknowledgement packet
                            dataPacket.isValidClock = false;
                        }

                        print("Clock is " + dataPacket.isValidClock + " with timestamp " + dataPacket.lamportClock);

                        packetManager.freeLock();
                        broadcaster.sendToClient(packetIn.client_id, (DataPacket) dataPacket);
                        break;
                    case DataPacket.CLIENT_ACK:
                       // clientAck();
                        int lamportClock = packetIn.lamportClock;
                        boolean clockIsValid = packetIn.isValidClock;

                        if(!clockIsValid){
                            // Update the current lamport clock
                            print("Awknowledgement failed. LC set to: " +  packetIn.lamportClock);
                            packetManager.setClockAndIndex(packetIn.lamportClock);
                        }

                        packetManager.freeSemaphore(1);
                        break;
                    case DataPacket.CLIENT_REGISTER:
                        try {
                            print("registerClientEvent");

                            /* Wait for handshaking packet from client, store client state in 
                             * global client table */
                            //DataPacket eventPacket2 = new DataPacket();

                            /* Add to client list */
                            dataPacket = setup(dataPacket, clientHandler.getMyId(), DataPacket.CLIENT_SPAWN, 
                            		packetManager.getLamportClock());
                            dataPacket.for_new_client = true;
                            dataPacket.NameServerTable = new ConcurrentHashMap();
                            dataPacket.NameServerTable.put(clientHandler.getMyId(), clientHandler.getMe());
                            dataPacket.client_score = clientHandler.getMyScore();

                            /* Get new client socket info */
                            String hostname = packetIn.client_host;
                            Integer id = packetIn.client_id;
                            int port = packetIn.client_port;
                            broadcaster.connectToPeer(id, hostname, port);

                            /* Add packet to event queue */
                            broadcaster.sendToClient(id,dataPacket);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_SPAWN:
                       // clientSpawn();
                        if (packetIn.for_new_client) {

                            // Update to the latest lamport clock
                            if(packetManager.getLamportClock() < packetIn.lamportClock)
                            		packetManager.setClockAndIndex(packetIn.lamportClock);
                	    

                            clientHandler.spawnClient(packetIn.client_id,packetIn.NameServerTable, packetIn.client_score);

                            packetManager.freeSemaphore(1);

                        } else { 
                            clientHandler.addEventToQueue(packetIn);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        }
                        break;
                    case DataPacket.CLIENT_FORWARD:
                        try { 
                            Integer id = packetIn.client_id;
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_FORWARD, packetIn.lamportClock);
                            print(id + " forward");
                            print("Lamport Clock "+ dataPacket.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_BACK:
                        try { 
                            Integer id = packetIn.client_id;
                            print(id + " back");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_BACK, packetIn.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_LEFT:
                        try { 
                            Integer id = packetIn.client_id;
                            print(id + " left");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_LEFT, packetIn.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_RIGHT:
                    	try { 
                            Integer id = packetIn.client_id;
                            print(id + " right");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_RIGHT, packetIn.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_FIRE:
                    	try { 
                            Integer id = packetIn.client_id;
                            print(id + " fire");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_FIRE, packetIn.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_RESPAWN:
                    	try { 
                            Point p = packetIn.client_location;
                            Direction d = packetIn.client_direction;

                            print(packetIn.target + " respawning");
                            
                            dataPacket = setup(dataPacket, packetIn.client_id, 
                            		DataPacket.CLIENT_RESPAWN, packetIn.lamportClock);
                            dataPacket.client_location = p;
                            dataPacket.client_direction = d;
                            dataPacket.shooter = packetIn.shooter;
                            dataPacket.target = packetIn.target;
                            clientHandler.clientRespawnEvent(packetIn);
           
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.RESERVE_POINT:
                    	/* Add to client list */
                        String remoteClientName = packetIn.client_name;
                        Point remoteClientLocation = packetIn.client_location;


                        DataPacket listenerResponse = new DataPacket();
                        listenerResponse.packet_type = DataPacket.RESERVE_POINT; 	    

                        if(packetManager.setPosition(remoteClientName,remoteClientLocation)){
                            listenerResponse.error_code = 0;
                            print("reserving position successful. " + remoteClientName );
                        }else{	
                            listenerResponse.error_code = DataPacket.ERROR_RESERVED_POSITION;
                            print("Reserving position failed. " + remoteClientName );
                        }

                        try{
                            outStream.writeObject(listenerResponse);

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("listener done broke");
                        }
                        continue;	  
                    case DataPacket.GET_SEQ_NUM:
                    	try { 
                            print("in getSeqNum");

                            dataPacket.packet_type = DataPacket.GET_SEQ_NUM;

                            clientHandler.addEventToQueue(dataPacket);
                        } catch (Exception e) {
                             e.printStackTrace();
                        }
                        continue;
                    case DataPacket.CLIENT_QUIT:
                    	try{
                    		dataPacket = setup(dataPacket, packetIn.client_id,
                    				DataPacket.CLIENT_REL_SEM, packetIn.lamportClock);
	                    
	                	    broadcaster.sendToClient(packetIn.client_id, dataPacket);
                            packetManager.removeSocketOutFromList(packetIn.client_id);
	
                            // Close all connections!
                            inStream.close();
                            outStream.close();
                            remoteS.close();

	                        clientHandler.addEventToQueue(packetIn);
	                	    clientHandler.runEventFromQueue(packetIn.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_REL_SEM:
                    	packetManager.freeSemaphore(1);
                    	System.out.println("Released a semaphore");
                    	break;
                    default:
                        System.out.println("Client Receiver Could not recognize packet type");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    public DataPacket setup (DataPacket dp, int clientID, int packetType, int clockLamport) {
    	dp.client_id = clientID;
	    dp.packet_type = packetType;
	    dp.lamportClock = clockLamport;
    	return dp;
    }
    public void print(String s) {
        if (DEBUG) {
            System.out.println("DEBUG: (ClientReceiverThread) " + s);
        }
    }

}
