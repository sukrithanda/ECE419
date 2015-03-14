import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/* Client handler 
 * Each client will be registered with a client handler

 Client connects to NameServer
 Client creates a thread to listen for incoming packets from other clients
 Client creates a broadcast thread to broadcast client state change

 * Listens for actions by GUI client and notifies listener
 * Receives game events queue from listener and executes events 
 * 
 */

public class MazewarP2PHandler extends Thread {
	
    Socket socket;
    Client me;
    int myId;
    Maze maze;
    ObjectOutputStream out;
    ObjectInputStream in;
    ConcurrentHashMap<String, Client> clientTable; 
    DataPacket [] eventQueue = new DataPacket[20];
    ConcurrentHashMap<Integer, DataPacket> lookupTable;

    int seqNum;
    boolean quitting = false;

    DataPacketManager data = new DataPacketManager();

    Broadcaster dispatcher = new Broadcaster(data, this);    

    DataPacket packetFromLookup = new DataPacket();
    DataPacket packetFromClient;

    boolean DEBUG = true;

    ScoreTableModel scoreModel;

    public MazewarP2PHandler(String nameserver_host, int nameserver_port, int client_port, ScoreTableModel sm){
        /* Connect to naming service. */
        try {

            System.out.println("Connecting to Naming Service...");

            // Connect to NameServer
            socket = new Socket(nameserver_host,nameserver_port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            clientTable = new ConcurrentHashMap<String, Client>();	 
            scoreModel = sm;
       
            // Start client listener
            // 		- for other clients to connect to
            //		- to handle incoming packets
            ClientReciever mazewarListener = new ClientReciever(this,client_port, data, dispatcher);
            
            Thread thread = new Thread(mazewarListener);
            thread.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void registerMaze(Maze maze) {
        this.maze = maze;
    }


    public void registerClientWithLookup(int client_port, String name){
        DataPacket packetToLookup = new DataPacket();

        try{
            // Register self
            packetToLookup.scenarioType = DataPacket.NS_REGISTER;
            packetToLookup.playerType = DataPacket.PLAYER_REMOTE;
            packetToLookup.playerName = name;
            packetToLookup.hostName = InetAddress.getLocalHost().getHostName();
            packetToLookup.portNum = client_port;

            out.writeObject(packetToLookup);

            packetFromLookup = (DataPacket) in.readObject();

            lookupRegisterEvent();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR: registering with listener");
        }

    }

    public void registerRobotWithMazewar(Client name){
        DataPacket packetToLookup = new DataPacket();

        try{

            /* Initialize handshaking with listener */
            Random rand = new Random();

            packetToLookup.scenarioType = DataPacket.PLAYER_REGISTER;
            packetToLookup.playerName = me.getName();
            packetToLookup.playerLocation = maze.getClientPoint(name);
            packetToLookup.playerDirection = me.getOrientation();
            packetToLookup.playerType = DataPacket.PLAYER_REMOTE;
            System.out.println("CLIENT REGISTER: " + me.getName());
            out.writeObject(packetToLookup);

            /* Init client table with yourself */
            clientTable.put(me.getName(), me);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("ERROR: registering with listener");
        }

    }

    public void broadcastNewClient(){
        System.out.println("Broadcasting CLIENT_REGISTER");

        DataPacket packetToClients = new DataPacket();

        packetToClients.scenarioType = DataPacket.PLAYER_REGISTER;
        packetToClients.playerID = myId; 
        packetToClients.hostName = lookupTable.get(myId).hostName;
        packetToClients.portNum = lookupTable.get(myId).portNum;  

        dispatcher.peerSendMulticast(packetToClients);
    }

    public void broadcastNewClientLocation(){
        DataPacket cd = new DataPacket();
        cd = getMe();

        DataPacket packetToClients = new DataPacket();

        packetToClients.scenarioType = DataPacket.PLAYER_SPAWN;
        packetToClients.newPlayer = false;
        packetToClients.playerID = myId;
        packetToClients.NSTable = new ConcurrentHashMap();
        packetToClients.NSTable.put(myId,getMe());

        cd.player = me;
        cd.player.setId(myId);
        lookupTable.put(myId,cd);

        dispatcher.peerSendMulticast(packetToClients);

    }

    // Check if registration successful
    private void lookupRegisterEvent(){
    	// Check if there is an error
    	if(packetFromLookup.errType == DataPacket.ERR_RESERVED_NS_PORT){
    		System.out.println("Try a different port!");
    		Mazewar.quit();
    	}
    	
        // Get the current lookup table
        lookupTable = new ConcurrentHashMap<Integer, DataPacket>();
        lookupTable = packetFromLookup.NSTable;

        myId = packetFromLookup.playerID;
        //data.addSocketOutToList(myId, out);

        // Connect to all currently existing users
        // Save their out ports!
        if(!lookupTable.isEmpty()){
            Object[] keys = lookupTable.keySet().toArray();
            int size = lookupTable.size(); 

            // Connect to all client listeners, except for yourself
            for(int i = 0; i < size; i++){
                int key = Integer.parseInt(keys[i].toString());

                if (key == myId) continue;

                System.out.println("Adding client " + key);

                DataPacket client_data = lookupTable.get(key);
                String client_host = client_data.hostName;
                int client_port = client_data.portNum;

                Socket socket = null;
                ObjectOutputStream t_out = null;
                ObjectInputStream t_in = null;

                // Save socket out!
                try{
                    socket = new Socket(client_host, client_port);

                    t_out = new ObjectOutputStream(socket.getOutputStream());
                    //t_in = new ObjectInputStream(socket.getInputStream());

                    data.addOutputStream(t_out, key);

                    System.out.println("Success!");
                } catch(Exception e){
                    System.err.println("ERROR: Couldn't connect to currently existing client");
                }				    
            }
            broadcastNewClient();
        }
    }

    // Store all clients
    // ID, host name, port
    private void lookupGetEvent(){
        // May not need
    }

    //Remove the client that is quitting.
    private void clientQuitEvent(){	
        System.out.println("Remove quitting client");
	Client c = (lookupTable.get(packetFromClient.playerID)).player;
            maze.removeClient(c);
	    
    }

    private void clientRespawnEvent(){
        Integer t_id = packetFromClient.playerDead;
        Integer s_id = packetFromClient.playerFire;
        debug("in clientRespawnEvent(), shooter is " +  s_id + ", respawnning target " + t_id);

        if (lookupTable.containsKey(t_id)){
            Client tc = (lookupTable.get(t_id)).player;
            //tc.getLock();

            Client sc = (lookupTable.get(s_id)).player;
            sc.getLock();

            Point p = packetFromClient.playerLocation;
            Direction d = packetFromClient.playerDirection;

            maze.setClient(sc, tc, p,d);

            tc.setKilledTo(false);

            //tc.releaseLock();
            sc.releaseLock();

        } else {
            System.out.println("CLIENT: no client with id " +packetFromClient.playerID+ " in respawn");
        }
    }


    public void clientRespawnEvent(DataPacket packetFromClient){
        Integer t_id = packetFromClient.playerDead;
        Integer s_id = packetFromClient.playerFire;
        debug("in clientRespawnEvent(), shooter is " +  s_id + ", respawnning target " + t_id);

        if (lookupTable.containsKey(t_id)){
            Client tc = (lookupTable.get(t_id)).player;
            //tc.getLock();

            Client sc = (lookupTable.get(s_id)).player;
            sc.getLock();

            Point p = packetFromClient.playerLocation;
            Direction d = packetFromClient.playerDirection;

            maze.setClient(sc, tc, p,d);

            tc.setKilledTo(false);

            //tc.releaseLock();
	    sc.releaseLock();

        } else {
            System.out.println("CLIENT: no client with id " +packetFromClient.playerID+ " in respawn");
        }
    }

    /**
     * Process listener packet eventsi
     * */
    private void addClientEvent() {
        String name = packetFromLookup.playerName;
        ConcurrentHashMap<String, DataPacket> clientTableFromLookup = packetFromLookup.playerList;
        System.out.println("CLIENT: Lookup sent addClient event");

        if (name.equals(me.getName())) {
            System.out.println("CLIENT: Lookup added me!");
        }
        else {
            System.out.println("CLIENT: Lookup adding new client " + name);
            int clientType = packetFromLookup.playerType;

            switch (clientType) {
                case DataPacket.PLAYER_REMOTE:
                    //add remote client
                    RemoteClient c = new RemoteClient(name);
                    clientTable.put(name, c);
                    maze.addRemoteClient(c, packetFromLookup.playerLocation, packetFromLookup.playerDirection);
                    break;
                case DataPacket.PLAYER_ROBOT:
                    //add robot client
                    break;
                default:
                    System.out.println("CLIENT: no new clients on add client event");
                    break;
            }
        }

        seqNum = packetFromLookup.orderVal;

        // else listener is telling you to add a new client
        // create new clients into clientTable based on any
        // new clients seen in clientTableFromLookup
        for (Map.Entry<String, DataPacket> entry : clientTableFromLookup.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            if (!clientTable.containsKey(key)) {
                DataPacket cData = entry.getValue();

                switch (cData.playerType) {
                    case DataPacket.PLAYER_REMOTE:
                        //add remote client
                        RemoteClient c = new RemoteClient(key);
                        clientTable.put(key, c);
                        maze.addRemoteClient(c, cData.playerLocation, cData.playerDirection);
                        break;
                    case DataPacket.PLAYER_ROBOT:
                        //add robot client
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void clientForwardEvent(Client c) {
        if (!c.isKilled()) { 
            c.forward();
        } else {
            System.out.println("CLIENT: no client " +packetFromClient.playerID+ " in forward");
        }
    }

    private void clientBackEvent(Client c) {
        if (!c.isKilled()) { 
            c.backup();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.playerID+ " in backup");
        }
    }

    private void clientLeftEvent(Client c) {
        if (!c.isKilled()) { 
            c.turnLeft();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.playerID+ " in left");
        }
    }

    private void clientRightEvent(Client c) {
        if (!c.isKilled()) { 
            c.turnRight();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.playerID+ " in right");
        }
    }


    private void clientFireEvent(Client c) {
        if (!c.isKilled()) { 
            c.fire();
            // Decrement score.
            //scoreTable.clientFired(clientTable.get(name));

        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.playerID+ " in fire");
        }
    }

    /**
     * Listen for client keypress and send listener packets 
     * */
    public void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            System.out.println("CLIENT: Quitting");

            quitting = true;

            try{
				// Send to other clients you are quitting
				DataPacket packetToClients = new DataPacket();
				packetToClients.scenarioType = DataPacket.PLAYER_QUIT;
				packetToClients.playerID = myId;
				dispatcher.peerSendMulticast(packetToClients);
		
				// Don't exit until you have recieved all acknowledgements
				//data.acquireSemaphore(data.socketOutList.size());;
		
				// Send lookup that you are quitting
				DataPacket packetToLookup = new DataPacket();
				packetToLookup.scenarioType = DataPacket.NS_QUIT;
				packetToLookup.playerID = myId;
				out.writeObject(packetToLookup);
				System.out.println("Client quit from lookup.");
		
				System.out.println("Client about to leave.");
		
				// Close lookup connection.
		                out.close();
		                in.close();
		                socket.close();
		
				// Close client connections.
				// data.quit();

            } catch(Exception e1){
                System.out.println("CLIENT: Couldn't close sockets...");
            }

            Mazewar.quit();
            // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP && !me.isKilled()) {
            sendPacketToClients(DataPacket.PLAYER_FORWARD);
            // Down-arrow moves backward.
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN && !me.isKilled()) {
            sendPacketToClients(DataPacket.PLAYER_BACK);
            //backup();
            // Left-arrow turns left.
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT && !me.isKilled()) {
            sendPacketToClients(DataPacket.PLAYER_LEFT);
            //turnLeft();
            // Right-arrow turns right.
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT && !me.isKilled()) {
            sendPacketToClients(DataPacket.PLAYER_RIGHT);
            //turnRight();
            // Spacebar fires.
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE && !me.isKilled()) {
            sendPacketToClients(DataPacket.PLAYER_FIRE);
            //fire();
        }
    }

    private void sendPacketToClients(int packetType) {
        DataPacket packetToClients = new DataPacket();
        packetToClients.scenarioType = packetType;
        packetToClients.playerName = me.getName();
        packetToClients.playerID = myId;

        dispatcher.peerSendMulticast(packetToClients);  
    }

    // Try and reserve a point!
    public boolean reservePoint(Point point){
        DataPacket packetToLookup = new DataPacket();

        // try{
        //     packetToLookup.packet_type = MazePacket.RESERVE_POINT;
        //     packetToLookup.client_name = me.getName();
        //     packetToLookup.client_location = point;
        //     packetToLookup.client_direction = null;
        //     packetToLookup.client_type = MazePacket.REMOTE;
        //     System.out.println("CLIENT " + me.getName() + " RESERVING POINT");
        //     out.writeObject(packetToLookup);

        //     packetFromLookup = new MazePacket();
        //     packetFromLookup = (MazePacket) in.readObject();

        //     int error_code = packetFromLookup.error_code;

        //     if(error_code == 0)
        // 	return true;
        //     else
        //     	return false;


        // }catch (Exception e){
        //     e.printStackTrace();
        //     System.out.println("ERROR: reserving point");
        //     return false;
        // }
        return true;
    }

    public boolean clientIsMe(Client c){
        if(c == me)
            return true;
        else
            return false;
    }


    public void sendClientRespawn(Integer sc, Integer tc, Point p, Direction d) {
        debug("I just died. in sendClientRespawn");
        debug(String.format("in sendClientRespawnEvent, params: %d %d", sc, tc));
        DataPacket respawnPacket = new DataPacket();
        respawnPacket.playerID = myId;
        respawnPacket.scenarioType = DataPacket.PLAYER_RESPAWN;
        respawnPacket.playerFire = sc;
        respawnPacket.playerDead = tc;
        respawnPacket.playerLocation = p;
        respawnPacket.playerDirection = d;
        dispatcher.peerSendMulticast(respawnPacket);
    }

    public int getMyScore(){
    	return scoreModel.getScore(lookupTable.get(myId).player);
    }

    public void spawnClient(Integer id, ConcurrentHashMap<Integer,DataPacket> tuple, int score){
        DataPacket cd = new DataPacket();
        cd = tuple.get(id);

        // Spawn client	
        RemoteClient c = new RemoteClient(cd.playerName);
        maze.addRemoteClient(c, cd.playerLocation, cd.playerDirection);

		// Update score
		scoreModel.setScore(c,score);

        // Update tuple
        cd.player = c;
        cd.player.setId(id);
        lookupTable.put(id, cd);
    }

    public void spawnClient(){
        Integer id = packetFromClient.playerID;
        ConcurrentHashMap<Integer,DataPacket> tuple = packetFromClient.NSTable;

        DataPacket cd = new DataPacket();
        cd = tuple.get(id);

        // Spawn client	
        RemoteClient c = new RemoteClient(cd.playerName);
        maze.addRemoteClient(c, cd.playerLocation, cd.playerDirection);

        // Update tuple
        cd.player = c;
        cd.player.setId(id);
        lookupTable.put(id, cd);
    }

    public DataPacket getMe() {
        DataPacket DataPacket = new DataPacket();
        DataPacket.playerID = myId;
        DataPacket.playerName = me.getName();
        DataPacket.playerLocation = maze.getClientPoint(me);
        DataPacket.playerDirection = me.getOrientation();
        return DataPacket;
    }

    public Integer getMyId() {
        return myId;
    }

    public void addEventToQueue(DataPacket p) {
        System.out.println("CHANDLER: Saving event at index: " + p.lampClk);
        eventQueue[p.lampClk] =  p;
    }

    public void runEventFromQueue(Integer lc){
        boolean executed;
        Integer currentLC = data.getOpOrder();
        System.out.println("CHANDLER: in runEventFromQueue, got lamportClock " + lc + ", current eventIndex is " + currentLC);

        if (data.getOpOrder() == lc) {
            int i = lc;
            while (eventQueue[i] != null) {
                System.out.println("CHANDLER: running event with lc = " + i);
                packetFromClient = eventQueue[lc];
                eventQueue[lc] = null;

                Client c = null;
                if(packetFromClient.scenarioType != DataPacket.PLAYER_SPAWN)
                	if(packetFromClient.scenarioType != DataPacket.PLAYER_QUIT)
                		c = (lookupTable.get(packetFromClient.playerID)).player;

                executed = executeEvent(c);
                if (!executed) break;

                i = i + 1;
                if(i == 20)
                	i = 0;
            }
            System.out.println("CHANDLER: eventIndex is now  " + i);
            data.setOpOrder(i);
            //if(packetFromClient.client_id != myId)
            //    data.incrementLamportClock();
        } 
    }

    private boolean executeEvent(Client c) {
        boolean status = true;

        if(c != null)
            c.getLock();		

        int type = packetFromClient.scenarioType;
        
        if (DataPacket.PLAYER_LEFT == type) {
        	
        	clientLeftEvent(c);
        	
        } else if (DataPacket.PLAYER_RIGHT == type) {
        	
        	clientRightEvent(c);
        	
        } else if (DataPacket.PLAYER_BACK == type) {
        	
        	clientBackEvent(c);
        	
        } else if (DataPacket.PLAYER_FORWARD == type) {
        	
        	clientForwardEvent(c);
        	
        } else if (DataPacket.PLAYER_SPAWN == type) {
        	
        	spawnClient();
        	
        } else if (DataPacket.PLAYER_RESPAWN == type) {
        	
        	clientRespawnEvent();
        	
        } else if (DataPacket.PLAYER_FIRE == type) {
        	
        	clientFireEvent(c);
        	
        } else if (DataPacket.PLAYER_REGISTER == type) {
        	
        	addClientEvent();
        	
        } else if (DataPacket.PLAYER_QUIT == type) {
        	
        	clientQuitEvent();
        	
        } else {
            status = false;
            System.out.println("ERROR: UNKNOWN PACKET TYPE" + packetFromClient.scenarioType);
        }

        if(c != null)
            c.releaseLock();

        System.out.println("SYNCRONIZATION: LOCK RELEASED");
        return status;
    }

    //BABNEET - REMOVE
    private void debug(String s) {
        if (DEBUG) {
            System.out.println("DEBUG: (ClientHandlerThread) " + s);
        }
    }

}


