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

public class ClientHandlerThread extends Thread {
	
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

    public ClientHandlerThread(String nameserver_host, int nameserver_port, int client_port, ScoreTableModel sm){
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
            packetToLookup.packet_type = DataPacket.LOOKUP_REGISTER;
            packetToLookup.client_type = DataPacket.REMOTE;
            packetToLookup.client_name = name;
            packetToLookup.client_host = InetAddress.getLocalHost().getHostName();
            packetToLookup.client_port = client_port;

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

            packetToLookup.packet_type = DataPacket.CLIENT_REGISTER;
            packetToLookup.client_name = me.getName();
            packetToLookup.client_location = maze.getClientPoint(name);
            packetToLookup.client_direction = me.getOrientation();
            packetToLookup.client_type = DataPacket.REMOTE;
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

        packetToClients.packet_type = DataPacket.CLIENT_REGISTER;
        packetToClients.client_id = myId; 
        packetToClients.client_host = lookupTable.get(myId).client_host;
        packetToClients.client_port = lookupTable.get(myId).client_port;  

        dispatcher.send(packetToClients);
    }

    public void broadcastNewClientLocation(){
        DataPacket cd = new DataPacket();
        cd = getMe();

        DataPacket packetToClients = new DataPacket();

        packetToClients.packet_type = DataPacket.CLIENT_SPAWN;
        packetToClients.for_new_client = false;
        packetToClients.client_id = myId;
        packetToClients.NameServerTable = new ConcurrentHashMap();
        packetToClients.NameServerTable.put(myId,getMe());

        cd.c = me;
        cd.c.setId(myId);
        lookupTable.put(myId,cd);

        dispatcher.send(packetToClients);

    }

    // Check if registration successful
    private void lookupRegisterEvent(){
    	// Check if there is an error
    	if(packetFromLookup.error_code == DataPacket.ERROR_LOOKUP_PORT){
    		System.out.println("Try a different port!");
    		Mazewar.quit();
    	}
    	
        // Get the current lookup table
        lookupTable = new ConcurrentHashMap<Integer, DataPacket>();
        lookupTable = packetFromLookup.NameServerTable;

        myId = packetFromLookup.client_id;
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
                String client_host = client_data.client_host;
                int client_port = client_data.client_port;

                Socket socket = null;
                ObjectOutputStream t_out = null;
                ObjectInputStream t_in = null;

                // Save socket out!
                try{
                    socket = new Socket(client_host, client_port);

                    t_out = new ObjectOutputStream(socket.getOutputStream());
                    //t_in = new ObjectInputStream(socket.getInputStream());

                    data.addSocketOutToList(key, t_out);

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
	Client c = (lookupTable.get(packetFromClient.client_id)).c;
            maze.removeClient(c);
	    
    }

    private void clientRespawnEvent(){
        Integer t_id = packetFromClient.target;
        Integer s_id = packetFromClient.shooter;
        debug("in clientRespawnEvent(), shooter is " +  s_id + ", respawnning target " + t_id);

        if (lookupTable.containsKey(t_id)){
            Client tc = (lookupTable.get(t_id)).c;
            //tc.getLock();

            Client sc = (lookupTable.get(s_id)).c;
            sc.getLock();

            Point p = packetFromClient.client_location;
            Direction d = packetFromClient.client_direction;

            maze.setClient(sc, tc, p,d);

            tc.setKilledTo(false);

            //tc.releaseLock();
            sc.releaseLock();

        } else {
            System.out.println("CLIENT: no client with id " +packetFromClient.client_id+ " in respawn");
        }
    }


    public void clientRespawnEvent(DataPacket packetFromClient){
        Integer t_id = packetFromClient.target;
        Integer s_id = packetFromClient.shooter;
        debug("in clientRespawnEvent(), shooter is " +  s_id + ", respawnning target " + t_id);

        if (lookupTable.containsKey(t_id)){
            Client tc = (lookupTable.get(t_id)).c;
            //tc.getLock();

            Client sc = (lookupTable.get(s_id)).c;
            sc.getLock();

            Point p = packetFromClient.client_location;
            Direction d = packetFromClient.client_direction;

            maze.setClient(sc, tc, p,d);

            tc.setKilledTo(false);

            //tc.releaseLock();
	    sc.releaseLock();

        } else {
            System.out.println("CLIENT: no client with id " +packetFromClient.client_id+ " in respawn");
        }
    }

    /**
     * Process listener packet eventsi
     * */
    private void addClientEvent() {
        String name = packetFromLookup.client_name;
        ConcurrentHashMap<String, DataPacket> clientTableFromLookup = packetFromLookup.client_list;
        System.out.println("CLIENT: Lookup sent addClient event");

        if (name.equals(me.getName())) {
            System.out.println("CLIENT: Lookup added me!");
        }
        else {
            System.out.println("CLIENT: Lookup adding new client " + name);
            int clientType = packetFromLookup.client_type;

            switch (clientType) {
                case DataPacket.REMOTE:
                    //add remote client
                    RemoteClient c = new RemoteClient(name);
                    clientTable.put(name, c);
                    maze.addRemoteClient(c, packetFromLookup.client_location, packetFromLookup.client_direction);
                    break;
                case DataPacket.ROBOT:
                    //add robot client
                    break;
                default:
                    System.out.println("CLIENT: no new clients on add client event");
                    break;
            }
        }

        seqNum = packetFromLookup.sequence_num;

        // else listener is telling you to add a new client
        // create new clients into clientTable based on any
        // new clients seen in clientTableFromLookup
        for (Map.Entry<String, DataPacket> entry : clientTableFromLookup.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            if (!clientTable.containsKey(key)) {
                DataPacket cData = entry.getValue();

                switch (cData.client_type) {
                    case DataPacket.REMOTE:
                        //add remote client
                        RemoteClient c = new RemoteClient(key);
                        clientTable.put(key, c);
                        maze.addRemoteClient(c, cData.client_location, cData.client_direction);
                        break;
                    case DataPacket.ROBOT:
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
            System.out.println("CLIENT: no client " +packetFromClient.client_id+ " in forward");
        }
    }

    private void clientBackEvent(Client c) {
        if (!c.isKilled()) { 
            c.backup();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.client_id+ " in backup");
        }
    }

    private void clientLeftEvent(Client c) {
        if (!c.isKilled()) { 
            c.turnLeft();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.client_id+ " in left");
        }
    }

    private void clientRightEvent(Client c) {
        if (!c.isKilled()) { 
            c.turnRight();
        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.client_id+ " in right");
        }
    }


    private void clientFireEvent(Client c) {
        if (!c.isKilled()) { 
            c.fire();
            // Decrement score.
            //scoreTable.clientFired(clientTable.get(name));

        } else {
            System.out.println("CLIENT: no client named " +packetFromClient.client_id+ " in fire");
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
				packetToClients.packet_type = DataPacket.CLIENT_QUIT;
				packetToClients.client_id = myId;
				dispatcher.send(packetToClients);
		
				// Don't exit until you have recieved all acknowledgements
				//data.acquireSemaphore(data.socketOutList.size());;
		
				// Send lookup that you are quitting
				DataPacket packetToLookup = new DataPacket();
				packetToLookup.packet_type = DataPacket.LOOKUP_QUIT;
				packetToLookup.client_id = myId;
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
            sendPacketToClients(DataPacket.CLIENT_FORWARD);
            // Down-arrow moves backward.
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN && !me.isKilled()) {
            sendPacketToClients(DataPacket.CLIENT_BACK);
            //backup();
            // Left-arrow turns left.
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT && !me.isKilled()) {
            sendPacketToClients(DataPacket.CLIENT_LEFT);
            //turnLeft();
            // Right-arrow turns right.
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT && !me.isKilled()) {
            sendPacketToClients(DataPacket.CLIENT_RIGHT);
            //turnRight();
            // Spacebar fires.
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE && !me.isKilled()) {
            sendPacketToClients(DataPacket.CLIENT_FIRE);
            //fire();
        }
    }

    private void sendPacketToClients(int packetType) {
        DataPacket packetToClients = new DataPacket();
        packetToClients.packet_type = packetType;
        packetToClients.client_name = me.getName();
        packetToClients.client_id = myId;

        dispatcher.send(packetToClients);  
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
        respawnPacket.client_id = myId;
        respawnPacket.packet_type = DataPacket.CLIENT_RESPAWN;
        respawnPacket.shooter = sc;
        respawnPacket.target = tc;
        respawnPacket.client_location = p;
        respawnPacket.client_direction = d;
        dispatcher.send(respawnPacket);
    }

    public int getMyScore(){
    	return scoreModel.getScore(lookupTable.get(myId).c);
    }

    public void spawnClient(Integer id, ConcurrentHashMap<Integer,DataPacket> tuple, int score){
        DataPacket cd = new DataPacket();
        cd = tuple.get(id);

        // Spawn client	
        RemoteClient c = new RemoteClient(cd.client_name);
        maze.addRemoteClient(c, cd.client_location, cd.client_direction);

		// Update score
		scoreModel.setScore(c,score);

        // Update tuple
        cd.c = c;
        cd.c.setId(id);
        lookupTable.put(id, cd);
    }

    public void spawnClient(){
        Integer id = packetFromClient.client_id;
        ConcurrentHashMap<Integer,DataPacket> tuple = packetFromClient.NameServerTable;

        DataPacket cd = new DataPacket();
        cd = tuple.get(id);

        // Spawn client	
        RemoteClient c = new RemoteClient(cd.client_name);
        maze.addRemoteClient(c, cd.client_location, cd.client_direction);

        // Update tuple
        cd.c = c;
        cd.c.setId(id);
        lookupTable.put(id, cd);
    }

    public DataPacket getMe() {
        DataPacket DataPacket = new DataPacket();
        DataPacket.client_id = myId;
        DataPacket.client_name = me.getName();
        DataPacket.client_location = maze.getClientPoint(me);
        DataPacket.client_direction = me.getOrientation();
        return DataPacket;
    }

    public Integer getMyId() {
        return myId;
    }

    public void addEventToQueue(DataPacket p) {
        System.out.println("CHANDLER: Saving event at index: " + p.lamportClock);
        eventQueue[p.lamportClock] =  p;
    }

    public void runEventFromQueue(Integer lc){
        boolean executed;
        Integer currentLC = data.getMoveIndex();
        System.out.println("CHANDLER: in runEventFromQueue, got lamportClock " + lc + ", current eventIndex is " + currentLC);

        if (data.getMoveIndex() == lc) {
            int i = lc;
            while (eventQueue[i] != null) {
                System.out.println("CHANDLER: running event with lc = " + i);
                packetFromClient = eventQueue[lc];
                eventQueue[lc] = null;

                Client c = null;
                if(packetFromClient.packet_type != DataPacket.CLIENT_SPAWN)
                	if(packetFromClient.packet_type != DataPacket.CLIENT_QUIT)
                		c = (lookupTable.get(packetFromClient.client_id)).c;

                executed = executeEvent(c);
                if (!executed) break;

                i = i + 1;
                if(i == 20)
                	i = 0;
            }
            System.out.println("CHANDLER: eventIndex is now  " + i);
            data.setMoveIndex(i);
            //if(packetFromClient.client_id != myId)
            //    data.incrementLamportClock();
        } 
    }

    private boolean executeEvent(Client c) {
        boolean status = true;

        if(c != null)
            c.getLock();		

        int type = packetFromClient.packet_type;
        
        if (DataPacket.CLIENT_LEFT == type) {
        	
        	clientLeftEvent(c);
        	
        } else if (DataPacket.CLIENT_RIGHT == type) {
        	
        	clientRightEvent(c);
        	
        } else if (DataPacket.CLIENT_BACK == type) {
        	
        	clientBackEvent(c);
        	
        } else if (DataPacket.CLIENT_FORWARD == type) {
        	
        	clientForwardEvent(c);
        	
        } else if (DataPacket.CLIENT_SPAWN == type) {
        	
        	spawnClient();
        	
        } else if (DataPacket.CLIENT_RESPAWN == type) {
        	
        	clientRespawnEvent();
        	
        } else if (DataPacket.CLIENT_FIRE == type) {
        	
        	clientFireEvent(c);
        	
        } else if (DataPacket.CLIENT_REGISTER == type) {
        	
        	addClientEvent();
        	
        } else if (DataPacket.CLIENT_QUIT == type) {
        	
        	clientQuitEvent();
        	
        } else {
            status = false;
            System.out.println("ERROR: UNKNOWN PACKET TYPE" + packetFromClient.packet_type);
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


