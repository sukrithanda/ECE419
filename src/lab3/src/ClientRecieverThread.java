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
    int logical_CLK;

    MazewarP2PHandler clientHandler;
    Broadcaster broadcaster;

    DataPacketManager packetManager = null;
    Socket remoteS = null;

    public ClientRecieverThread (MazewarP2PHandler clientHandler, Socket socket, 
    		DataPacketManager listener, Broadcaster broadcaster) throws IOException {
        try {
        	print("Starting");
            this.remoteS = socket;
            this.broadcaster = broadcaster;
            this.clientHandler = clientHandler;
            this.packetManager = listener;
            this.outStream = new ObjectOutputStream(remoteS.getOutputStream());
        	print("contructor finished- ClientReceiverThread");
        } catch (IOException e) {
        	print("ERROR: IO exception");
        }
    }

    boolean kill = false;
    int test = 1;
    
    public void print(String str) {
        if (test == 1) {
            System.out.println("DEBUG: (ClientReceiverThread) " + str);
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
                        int requested_lc = packetIn.lampClk;    
                        dataPacket.scenarioType = DataPacket.PLAYER_ACK;
                        packetManager.acqLock();

                        print("requested_lc: " + requested_lc + " current lamportClock: " + packetManager.getLampClk());
                        dataPacket.lampClk = packetManager.getLampClk();

                        if(requested_lc >= logical_CLK){
                            print("incrementing my lc after recieving CLIENT_CLOCK packet");
                            // Clock is valid!
                            if(requested_lc == 19){
                            	packetManager.initLampClk(0);
                            } else {
                            	packetManager.initLampClk(++requested_lc);
                            }
                            dataPacket.clkCredible = true;
                            print("Incremented lc is " + packetManager.getLampClk());

                        } else{
     
                            // Send the latest lamport clock and disawknowledgement packet
                            dataPacket.clkCredible = false;
                        }

                        print("Clock is " + dataPacket.clkCredible + " with timestamp " + dataPacket.lampClk);

                        packetManager.freeLock();
                        broadcaster.peerSend((DataPacket) dataPacket, packetIn.playerID);
                        break;
                    case DataPacket.PLAYER_ACK:
                        boolean clockIsValid = packetIn.clkCredible;
                        if(!clockIsValid){
                            print("Awknowledgement failed. LC set to: " +  packetIn.lampClk);
                            // Update the current lamport clock
                            packetManager.setLampClkIndex(packetIn.lampClk);
                        }

                        packetManager.freeSemaphore(1);
                        break;
                    case DataPacket.PLAYER_REGISTER:
                        try {
                            print("registerClientEvent");

                            /* Wait for handshaking packet from client, store client state in 
                             * global client table */
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
                        if (packetIn.newPlayer) {
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
                            print("listener done broke");
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
                    	print("Freed semaphore");
                    	break;
                    default:
                        print("Unkown type of DataPacket");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            print("thread not working");
        }
    }

    public DataPacket setup (DataPacket dp, int clientID, int packetType, int clockLamport) {
    	dp.playerID = clientID;
	    dp.scenarioType = packetType;
	    dp.lampClk = clockLamport;
    	return dp;
    }
}
