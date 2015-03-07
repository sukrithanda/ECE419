import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.io.*;
import java.net.*;

import javax.xml.crypto.Data;

/*
 * ClientReceiverThread - Receives DataPackets from each remote client
 */

public class ClientRecieverThread extends Thread {
    DataPacket sendPacketRemoteClient;
    DataPacket receivedPacketRemoteClient;
    
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    
    ClientHandlerThread clientHandler;
    Broadcaster broadcaster;
    int clockLamport;

    DataPacketManager listenerData = null;
    Socket remoteClientSocket = null;

    boolean terminate = false;
    boolean DEBUG = true;

    public ClientRecieverThread (ClientHandlerThread clientHandler, Socket socket, 
    		DataPacketManager listenerData, Broadcaster broadcaster) throws IOException {
        try {
        	print("Starting");
            this.remoteClientSocket = socket;
            this.broadcaster = broadcaster;
            this.clientHandler = clientHandler;
            this.listenerData = listenerData;
            this.outputStream = new ObjectOutputStream(remoteClientSocket.getOutputStream());
        } catch (IOException e) {
            print("Exception");
        }
    }

    public void run() {
        try {
            inputStream = new ObjectInputStream(remoteClientSocket.getInputStream());
            while (((receivedPacketRemoteClient = (DataPacket) inputStream.readObject()) != null) && !terminate) {
                int type = receivedPacketRemoteClient.packet_type;
                print("Received Packet of type " + type);
                
                DataPacket dataPacket = new DataPacket();

                /* Process each packet */
                switch (receivedPacketRemoteClient.packet_type) {
                    case DataPacket.CLIENT_CLOCK:
                       // clientClock();
                        int requested_lc = receivedPacketRemoteClient.lamportClock;    
                        dataPacket.packet_type = DataPacket.CLIENT_ACK;
                        listenerData.acquireLock();

                        print("requested_lc: " + requested_lc + " current lamportClock: " + listenerData.getLamportClock());
                        dataPacket.lamportClock = listenerData.getLamportClock();

                        if(requested_lc >= clockLamport){
                            print("incrementing my lc after recieving CLIENT_CLOCK packet");
                            // Clock is valid!
                            if(requested_lc == 19){
                            	listenerData.setLamportClock(0);
                            } else {
                            	listenerData.setLamportClock(++requested_lc);
                            }
                            //Set up and send awknowledgement packet
                            //eventPacket.lamportClock = lamportClock;]
                            dataPacket.isValidClock = true;

                            print("Incremented lc is " + listenerData.getLamportClock());

                        } else{
                            // Oh no! The lamport clock is not valid.
                            // Send the latest lamport clock and disawknowledgement packet
                            dataPacket.isValidClock = false;
                        }

                        print("Clock is " + dataPacket.isValidClock + " with timestamp " + dataPacket.lamportClock);

                        listenerData.freeLock();
                        broadcaster.sendToClient(receivedPacketRemoteClient.client_id, (DataPacket) dataPacket);
                        break;
                    case DataPacket.CLIENT_ACK:
                       // clientAck();
                        int lamportClock = receivedPacketRemoteClient.lamportClock;
                        boolean clockIsValid = receivedPacketRemoteClient.isValidClock;

                        if(!clockIsValid){
                            // Update the current lamport clock
                            print("Awknowledgement failed. LC set to: " +  receivedPacketRemoteClient.lamportClock);
                            listenerData.setClockAndIndex(receivedPacketRemoteClient.lamportClock);
                        }

                        listenerData.freeSemaphore(1);
                        break;
                    case DataPacket.CLIENT_REGISTER:
                        try {
                            print("registerClientEvent");

                            /* Wait for handshaking packet from client, store client state in 
                             * global client table */
                            //DataPacket eventPacket2 = new DataPacket();

                            /* Add to client list */
                            dataPacket = setup(dataPacket, clientHandler.getMyId(), DataPacket.CLIENT_SPAWN, 
                            		listenerData.getLamportClock());
                            dataPacket.for_new_client = true;
                            dataPacket.NameServerTable = new ConcurrentHashMap();
                            dataPacket.NameServerTable.put(clientHandler.getMyId(), clientHandler.getMe());
                            dataPacket.client_score = clientHandler.getMyScore();

                            /* Get new client socket info */
                            String hostname = receivedPacketRemoteClient.client_host;
                            Integer id = receivedPacketRemoteClient.client_id;
                            int port = receivedPacketRemoteClient.client_port;
                            broadcaster.connectToPeer(id, hostname, port);

                            /* Add packet to event queue */
                            broadcaster.sendToClient(id,dataPacket);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_SPAWN:
                       // clientSpawn();
                        if (receivedPacketRemoteClient.for_new_client) {

                            // Update to the latest lamport clock
                            if(listenerData.getLamportClock() < receivedPacketRemoteClient.lamportClock)
                            		listenerData.setClockAndIndex(receivedPacketRemoteClient.lamportClock);
                	    

                            clientHandler.spawnClient(receivedPacketRemoteClient.client_id,receivedPacketRemoteClient.NameServerTable, receivedPacketRemoteClient.client_score);

                            listenerData.freeSemaphore(1);

                        } else { 
                            clientHandler.addEventToQueue(receivedPacketRemoteClient);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        }
                        break;
                    case DataPacket.CLIENT_FORWARD:
                        try { 
                            Integer id = receivedPacketRemoteClient.client_id;
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_FORWARD, receivedPacketRemoteClient.lamportClock);
                            print(id + " forward");
                            print("Lamport Clock "+ dataPacket.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_BACK:
                        try { 
                            Integer id = receivedPacketRemoteClient.client_id;
                            print(id + " back");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_BACK, receivedPacketRemoteClient.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_LEFT:
                        try { 
                            Integer id = receivedPacketRemoteClient.client_id;
                            print(id + " left");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_LEFT, receivedPacketRemoteClient.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_RIGHT:
                    	try { 
                            Integer id = receivedPacketRemoteClient.client_id;
                            print(id + " right");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_RIGHT, receivedPacketRemoteClient.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_FIRE:
                    	try { 
                            Integer id = receivedPacketRemoteClient.client_id;
                            print(id + " fire");
                            dataPacket = setup(dataPacket, id, DataPacket.CLIENT_FIRE, receivedPacketRemoteClient.lamportClock);

                            clientHandler.addEventToQueue(dataPacket);
                            clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_RESPAWN:
                    	try { 
                            Point p = receivedPacketRemoteClient.client_location;
                            Direction d = receivedPacketRemoteClient.client_direction;

                            print(receivedPacketRemoteClient.target + " respawning");
                            
                            dataPacket = setup(dataPacket, receivedPacketRemoteClient.client_id, 
                            		DataPacket.CLIENT_RESPAWN, receivedPacketRemoteClient.lamportClock);
                            dataPacket.client_location = p;
                            dataPacket.client_direction = d;
                            dataPacket.shooter = receivedPacketRemoteClient.shooter;
                            dataPacket.target = receivedPacketRemoteClient.target;
                            clientHandler.clientRespawnEvent(receivedPacketRemoteClient);
           
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.RESERVE_POINT:
                    	/* Add to client list */
                        String remoteClientName = receivedPacketRemoteClient.client_name;
                        Point remoteClientLocation = receivedPacketRemoteClient.client_location;


                        DataPacket listenerResponse = new DataPacket();
                        listenerResponse.packet_type = DataPacket.RESERVE_POINT; 	    

                        if(listenerData.setPosition(remoteClientName,remoteClientLocation)){
                            listenerResponse.error_code = 0;
                            print("reserving position successful. " + remoteClientName );
                        }else{	
                            listenerResponse.error_code = DataPacket.ERROR_RESERVED_POSITION;
                            print("Reserving position failed. " + remoteClientName );
                        }

                        try{
                            outputStream.writeObject(listenerResponse);

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
                    		dataPacket = setup(dataPacket, receivedPacketRemoteClient.client_id,
                    				DataPacket.CLIENT_REL_SEM, receivedPacketRemoteClient.lamportClock);
	                    
	                	    broadcaster.sendToClient(receivedPacketRemoteClient.client_id, dataPacket);
                            listenerData.removeSocketOutFromList(receivedPacketRemoteClient.client_id);
	
                            // Close all connections!
                            inputStream.close();
                            outputStream.close();
                            remoteClientSocket.close();

	                        clientHandler.addEventToQueue(receivedPacketRemoteClient);
	                	    clientHandler.runEventFromQueue(receivedPacketRemoteClient.lamportClock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.CLIENT_REL_SEM:
                    	listenerData.freeSemaphore(1);
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
