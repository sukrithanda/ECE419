import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.io.*;
import java.net.*;

/* MazewarListenerHandlerThread class
 *
 * MazewarListener spawns this thread for each remote
 * client that connects
 */

public class MazewarListenerHandlerThread extends Thread {
    Socket rcSocket = null;
    ListenerData data = null;

    ObjectInputStream cin;
    ObjectOutputStream cout;

    DataPacket packetFromRC;
    DataPacket packetToRC;

    Random rand = new Random();

    Integer myId;
    int seqNum = 1;
    boolean quitting = false;

    int lamportClock;
    Dispatcher dispatcher;
    ClientHandlerThread chandler;

    boolean debug = true;

    public MazewarListenerHandlerThread (Socket socket, ListenerData sdata, Dispatcher dispatcher, ClientHandlerThread chandler) throws IOException {
        super("MazewarListenerHandlerThread");
        try {
            this.rcSocket = socket;
            this.cout = new ObjectOutputStream(rcSocket.getOutputStream());
            this.data = sdata;
            this.dispatcher = dispatcher;
            //data.addSocketOutToList(cout);

            this.chandler = chandler;

            System.out.println("Created new MazewarListenerHandlerThread to handle remote client ");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public void run() {
        System.out.println("Connecting to client...");
        int lastPacketNum;

        try {
            /* Loop: 
            */
            cin = new ObjectInputStream(rcSocket.getInputStream());
            while (!quitting && ((packetFromRC = (DataPacket) cin.readObject()) != null)) {
                debug("packet type is " + packetFromRC.packet_type);

                /* Process each packet */
                switch (packetFromRC.packet_type) {
                    case DataPacket.CLIENT_CLOCK:
                        clientClock();
                        break;
                    case DataPacket.CLIENT_ACK:
                        clientAck();
                        break;
                    case DataPacket.CLIENT_REGISTER:
                        registerClientEvent();
                        break;
                    case DataPacket.CLIENT_SPAWN:
                        clientSpawn();
                        break;
                    case DataPacket.CLIENT_FORWARD:
                        clientForwardEvent();
                        break;
                    case DataPacket.CLIENT_BACK:
                        clientBackEvent();
                        break;
                    case DataPacket.CLIENT_LEFT:
                        clientLeftEvent();
                        break;
                    case DataPacket.CLIENT_RIGHT:
                        clientRightEvent();
                        break;
                    case DataPacket.CLIENT_FIRE:
                        clientFireEvent();
                        break;
                    case DataPacket.CLIENT_RESPAWN:
                        clientRespawn();
                        break;
                    case DataPacket.RESERVE_POINT:
                        reservePoint();
                        continue;	  
                    case DataPacket.GET_SEQ_NUM:
                        getSeqNum();
                        continue;
                    case DataPacket.CLIENT_QUIT:
                        clientQuit();
                        break;
		    case DataPacket.CLIENT_REL_SEM:
			clientReleaseSem();
			break;
                    default:
                        System.out.println("S_HANDLER: Could not recognize packet type");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientReleaseSem(){
	data.releaseSemaphore(1);
	System.out.println("Released a semaphore");
    }

    // Client is requesting for a valid clock!
    private void clientClock(){
        DataPacket eventPacket = new DataPacket();
        int requested_lc = packetFromRC.lamportClock;    
        //int lamportClock = dispatcher.getLamportClock();
        eventPacket.packet_type = DataPacket.CLIENT_ACK;
	data.getLock();

        debug("requested_lc: " + requested_lc + " current lamportClock: " + data.getLamportClock());
        eventPacket.lamportClock = data.getLamportClock();

        if(requested_lc >= lamportClock){
            debug("incrementing my lc after recieving CLIENT_CLOCK packet");
            // Clock is valid!
	    if(requested_lc == 19){
		data.setLamportClock(0);
	    } else {
		data.setLamportClock(requested_lc + 1);
	    }
            //Set up and send awknowledgement packet
            //eventPacket.lamportClock = lamportClock;]
            eventPacket.isValidClock = true;

            debug("Incremented lc is " + data.getLamportClock());

        } else{
            // Oh no! The lamport clock is not valid.
            // Send the latest lamport clock and disawknowledgement packet
            eventPacket.isValidClock = false;
        }

        debug("Clock is " + eventPacket.isValidClock + " with timestamp " + eventPacket.lamportClock);

	data.releaseLock();
        dispatcher.sendToClient(packetFromRC.client_id, (DataPacket) eventPacket);
    }

    private void clientSpawn(){

        //chandler.spawnClient(packetFromRC.client_id,packetFromRC.lookupTable);
        if (packetFromRC.for_new_client) {

            // Update to the latest lamport clock
            if(data.getLamportClock() < packetFromRC.lamportClock)
		data.setClockAndIndex(packetFromRC.lamportClock);
	    

            chandler.spawnClient(packetFromRC.client_id,packetFromRC.lookupTable, packetFromRC.client_score);

            data.releaseSemaphore(1);

        } else { 
            chandler.addEventToQueue(packetFromRC);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        }

    }

    // This client is awknowledging/disawk for lamport clock validation
    // Count the amount of awks
    private void clientAck(){
        int lamportClock = packetFromRC.lamportClock;
        boolean clockIsValid = packetFromRC.isValidClock;

        if(!clockIsValid){
            // Update the current lamport clock
            debug("Awknowledgement failed. LC set to: " +  packetFromRC.lamportClock);
	    //if(data.getLamportClock() < packetFromRC.lamportClock)
            	data.setClockAndIndex(packetFromRC.lamportClock);
        }

        data.releaseSemaphore(1);
	
    }

    // The client is quitting.
    private void clientQuit(){
        try{
            DataPacket eventPacket = new DataPacket();
	    eventPacket.client_id = packetFromRC.client_id;
	    eventPacket.packet_type = DataPacket.CLIENT_REL_SEM;
	    eventPacket.lamportClock = packetFromRC.lamportClock;
            //String rc_name = packetFromRC.client_name;
            //debug(rc_name + " is quitting");

            //eventPacket.client_name = rc_name;
            //eventPacket.packet_type = MazePacket.CLIENT_QUIT;

            //quitting = true;

            // Remove that client from the client table!
            //data.removeClientFromTable(rc_name);
	    dispatcher.sendToClient(packetFromRC.client_id, eventPacket);
            data.removeSocketOutFromList(packetFromRC.client_id);

            // Close all connections!
            cin.close();
            cout.close();
            rcSocket.close();

            chandler.addEventToQueue(packetFromRC);
	    chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }

    }

    private void getSeqNum(){
        try { 
            DataPacket eventPacket = new DataPacket();
            debug("in getSeqNum");

            eventPacket.packet_type = DataPacket.GET_SEQ_NUM;

            // TODO: handle this?
            chandler.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientForwardEvent() {
        try { 
            DataPacket eventPacket = new DataPacket();
            Integer id = packetFromRC.client_id;
            debug(id + " forward");

            eventPacket.client_id = id;
            eventPacket.packet_type = DataPacket.CLIENT_FORWARD;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            System.out.println("LISTENER: THIS IS THE LAMPORT CLOCK: "+ eventPacket.lamportClock);

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientBackEvent() {
        try { 
            DataPacket eventPacket = new DataPacket();
            Integer id = packetFromRC.client_id;
            debug(id + " back");

            eventPacket.client_id = id;
            eventPacket.packet_type = DataPacket.CLIENT_BACK;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientLeftEvent() {
        try { 
            DataPacket eventPacket = new DataPacket();
            Integer id = packetFromRC.client_id;
            debug(id + " left");

            eventPacket.client_id = id;
            eventPacket.packet_type = DataPacket.CLIENT_LEFT;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientRightEvent() {
        try { 
            DataPacket eventPacket = new DataPacket();
            Integer id = packetFromRC.client_id;
            debug(id + " right");

            eventPacket.client_id = id;
            eventPacket.packet_type = DataPacket.CLIENT_RIGHT;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientFireEvent() {
        try { 
            DataPacket eventPacket = new DataPacket();
            Integer id = packetFromRC.client_id;
            debug(id + " fire");

            eventPacket.client_id = id;
            eventPacket.packet_type = DataPacket.CLIENT_FIRE;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    /* A new client wants to register
     * - Give your current point and direction!
     */
    private void registerClientEvent() {
        try {
            debug("registerClientEvent");

            /* Wait for handshaking packet from client, store client state in 
             * global client table */
            DataPacket eventPacket = new DataPacket();

            /* Add to client list */
            eventPacket.client_id = chandler.getMyId();
            eventPacket.packet_type = DataPacket.CLIENT_SPAWN;
            eventPacket.for_new_client = true;
            eventPacket.lookupTable = new ConcurrentHashMap();
            eventPacket.lookupTable.put(chandler.getMyId(), chandler.getMe());
	    eventPacket.client_score = chandler.getMyScore();

            eventPacket.lamportClock = data.getLamportClock();

            /* Get new client socket info */
            String hostname = packetFromRC.client_host;
            Integer id = packetFromRC.client_id;
            int port = packetFromRC.client_port;
            dispatcher.connectToPeer(id, hostname, port);

            /* Add packet to event queue */
            dispatcher.sendToClient(id,eventPacket);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    /* When a client spawns.moves, it needs to reserve its position (aka. Point). You don't want multiple clients spawning in the same spot!
    */
    private void reservePoint(){
        // Check if another client currently has that position.

        /* Add to client list */
        String rc_name = packetFromRC.client_name;
        Point rc_point = packetFromRC.client_location;


        DataPacket listenerResponse = new DataPacket();
        listenerResponse.packet_type = DataPacket.RESERVE_POINT; 	    

        if(data.setPosition(rc_name,rc_point)){
            listenerResponse.error_code = 0;
            debug("reserving position successful. " + rc_name );
        }else{	
            listenerResponse.error_code = DataPacket.ERROR_RESERVED_POSITION;
            debug("Reserving position failed. " + rc_name );
        }

        try{
            cout.writeObject(listenerResponse);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    private void clientRespawn(){
        try { 
            DataPacket eventPacket = new DataPacket();
            Point p = packetFromRC.client_location;
            Direction d = packetFromRC.client_direction;

            debug(packetFromRC.target + " respawning");

            eventPacket.client_id = packetFromRC.client_id;
            eventPacket.lamportClock = packetFromRC.lamportClock;
            eventPacket.shooter = packetFromRC.shooter;
            eventPacket.target = packetFromRC.target;
            eventPacket.client_location = p;
            eventPacket.client_direction = d;
            eventPacket.packet_type = DataPacket.CLIENT_RESPAWN;

	    chandler.clientRespawnEvent(packetFromRC);
            // chandler.addEventToQueue(eventPacket);
            // chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("listener done broke");
        }
    }

    public void debug(String s) {
        if (debug) {
            System.out.println("S_HANDLER: " + s);
        }
    }

}
