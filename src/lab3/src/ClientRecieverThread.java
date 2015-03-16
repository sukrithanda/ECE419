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
    
    MazewarP2PHandler clientHandler;
    Broadcaster broadcaster;
    int laportCLK;

    DataPacketManager packetManager = null;
    Socket remoteS = null;

    boolean kill = false;
    boolean DEBUG = true;

    public ClientRecieverThread (MazewarP2PHandler clientHandler, Socket socket, 
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
                print("Received packet type: " + packetIn.scenarioType);
                
                DataPacket dataPacket = new DataPacket();

                /* Process each packet */
                switch (packetIn.scenarioType) {
                    case DataPacket.PLAYER_CLK:
                       // clientClock();
                        int requested_lc = packetIn.lampClk;    
                        dataPacket.scenarioType = DataPacket.PLAYER_ACK;
                        packetManager.acqLock();

                        print("requested_lc: " + requested_lc + " current lamportClock: " + packetManager.getLampClk());
                        dataPacket.lampClk = packetManager.getLampClk();

                        if(requested_lc >= laportCLK){
                            print("incrementing my lc after recieving CLIENT_CLOCK packet");
                            // Clock is valid!
                            if(requested_lc == 19){
                            	packetManager.initLampClk(0);
                            } else {
                            	packetManager.initLampClk(++requested_lc);
                            }
                            //Set up and send awknowledgement packet
                            //eventPacket.lamportClock = lamportClock;]
                            dataPacket.clkCredible = true;

                            print("Incremented lc is " + packetManager.getLampClk());

                        } else{
                            // Oh no! The lamport clock is not valid.
                            // Send the latest lamport clock and disawknowledgement packet
                            dataPacket.clkCredible = false;
                        }

                        print("Clock is " + dataPacket.clkCredible + " with timestamp " + dataPacket.lampClk);

                        packetManager.freeLock();
                        broadcaster.peerSend((DataPacket) dataPacket, packetIn.playerID);
                        break;
                    case DataPacket.PLAYER_ACK:
                       // clientAck();
                        int lamportClock = packetIn.lampClk;
                        boolean clockIsValid = packetIn.clkCredible;

                        if(!clockIsValid){
                            // Update the current lamport clock
                            print("Awknowledgement failed. LC set to: " +  packetIn.lampClk);
                            packetManager.setLampClkIndex(packetIn.lampClk);
                        }

                        packetManager.freeSemaphore(1);
                        break;
                    case DataPacket.PLAYER_REGISTER:
                        try {
                            print("registerClientEvent");

                            /* Wait for handshaking packet from client, store client state in 
                             * global client table */
                            //DataPacket eventPacket2 = new DataPacket();

                            /* Add to client list */
                            dataPacket = setup(dataPacket, clientHandler.localPlayerID(), DataPacket.PLAYER_SPAWN, 
                            		packetManager.getLampClk());
                            dataPacket.newPlayer = true;
                            dataPacket.NSTable = new ConcurrentHashMap();
                            dataPacket.NSTable.put(clientHandler.localPlayerID(), clientHandler.localPlayerInfo());
                            dataPacket.playerScore = clientHandler.localPlayerScore();

                            /* Get new client socket info */
                            String hostname = packetIn.hostName;
                            Integer id = packetIn.playerID;
                            int port = packetIn.portNum;
                            broadcaster.peerConnection(hostname, port, id);

                            /* Add packet to event queue */
                            broadcaster.peerSend(dataPacket,id);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_SPAWN:
                       // clientSpawn();
                        if (packetIn.newPlayer) {

                            // Update to the latest lamport clock
                            if(packetManager.getLampClk() < packetIn.lampClk)
                            		packetManager.setLampClkIndex(packetIn.lampClk);
                	    

                            clientHandler.playerSpawn(packetIn.playerScore,packetIn.playerID, packetIn.NSTable);

                            packetManager.freeSemaphore(1);

                        } else { 
                            clientHandler.recordPlayerOp(packetIn);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        }
                        break;
                    case DataPacket.PLAYER_FORWARD:
                        try { 
                            Integer id = packetIn.playerID;
                            dataPacket = setup(dataPacket, id, DataPacket.PLAYER_FORWARD, packetIn.lampClk);
                            print(id + " forward");
                            print("Lamport Clock "+ dataPacket.lampClk);

                            clientHandler.recordPlayerOp(dataPacket);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_BACK:
                        try { 
                            Integer id = packetIn.playerID;
                            print(id + " back");
                            dataPacket = setup(dataPacket, id, DataPacket.PLAYER_BACK, packetIn.lampClk);

                            clientHandler.recordPlayerOp(dataPacket);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_LEFT:
                        try { 
                            Integer id = packetIn.playerID;
                            print(id + " left");
                            dataPacket = setup(dataPacket, id, DataPacket.PLAYER_LEFT, packetIn.lampClk);

                            clientHandler.recordPlayerOp(dataPacket);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_RIGHT:
                    	try { 
                            Integer id = packetIn.playerID;
                            print(id + " right");
                            dataPacket = setup(dataPacket, id, DataPacket.PLAYER_RIGHT, packetIn.lampClk);

                            clientHandler.recordPlayerOp(dataPacket);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_FIRE:
                    	try { 
                            Integer id = packetIn.playerID;
                            print(id + " fire");
                            dataPacket = setup(dataPacket, id, DataPacket.PLAYER_FIRE, packetIn.lampClk);

                            clientHandler.recordPlayerOp(dataPacket);
                            clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_RESPAWN:
                    	try { 
                            Point p = packetIn.playerLocation;
                            Direction d = packetIn.playerDirection;

                            print(packetIn.playerDead + " respawning");
                            
                            dataPacket = setup(dataPacket, packetIn.playerID, 
                            		DataPacket.PLAYER_RESPAWN, packetIn.lampClk);
                            dataPacket.playerLocation = p;
                            dataPacket.playerDirection = d;
                            dataPacket.playerFire = packetIn.playerFire;
                            dataPacket.playerDead = packetIn.playerDead;
                            clientHandler.playerRespawn(packetIn);
           
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_SECURE_LOCATION:
                    	/* Add to client list */
                        String remoteClientName = packetIn.playerName;
                        Point remoteClientLocation = packetIn.playerLocation;


                        DataPacket listenerResponse = new DataPacket();
                        listenerResponse.scenarioType = DataPacket.PLAYER_SECURE_LOCATION; 	    

                        if(packetManager.recordPlayerOp(remoteClientLocation,remoteClientName)){
                            listenerResponse.errType = 0;
                            print("reserving position successful. " + remoteClientName );
                        }else{	
                            listenerResponse.errType = DataPacket.ERR_SECURED_LOCATION;
                            print("Reserving position failed. " + remoteClientName );
                        }

                        try{
                            outStream.writeObject(listenerResponse);

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("listener done broke");
                        }
                        continue;	  
                    case DataPacket.PLAYER_ORDER_VAL:
                    	try { 
                            print("in getSeqNum");

                            dataPacket.scenarioType = DataPacket.PLAYER_ORDER_VAL;

                            clientHandler.recordPlayerOp(dataPacket);
                        } catch (Exception e) {
                             e.printStackTrace();
                        }
                        continue;
                    case DataPacket.PLAYER_QUIT:
                    	try{
                    		dataPacket = setup(dataPacket, packetIn.playerID,
                    				DataPacket.PLAYER_FREE_SEMAPHORE, packetIn.lampClk);
	                    
	                	    broadcaster.peerSend(dataPacket, packetIn.playerID);
                            packetManager.deleteOutputStream(packetIn.playerID);
	
                            // Close all connections!
                            inStream.close();
                            outStream.close();
                            remoteS.close();

	                        clientHandler.recordPlayerOp(packetIn);
	                	    clientHandler.executePlayerOperation(packetIn.lampClk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case DataPacket.PLAYER_FREE_SEMAPHORE:
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
    	dp.playerID = clientID;
	    dp.scenarioType = packetType;
	    dp.lampClk = clockLamport;
    	return dp;
    }
    public void print(String s) {
        if (DEBUG) {
            System.out.println("DEBUG: (ClientReceiverThread) " + s);
        }
    }

}
