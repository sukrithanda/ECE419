import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* Dispatcher class
 * Dispatches messages from event queue and broadcasts
 * events to all remote clients.
 *
 */
public class Broadcaster extends Thread {
    LinkedBlockingQueue<DataPacket> movesQ = null;
    ConcurrentHashMap<String, DataPacket> clients= null;
    ConcurrentHashMap<Integer, ObjectOutputStream> outstreams = new ConcurrentHashMap<Integer, ObjectOutputStream>(); 
    int seqNum;

    int LCLK;
    Semaphore sem;
    DataPacketManager data;
    ClientHandlerThread chandler;

    Lock lock = new ReentrantLock();

    boolean debug = true;

    public Broadcaster(DataPacketManager data, ClientHandlerThread chandler) {
        this.data = data;
        this.chandler = chandler;
        this.movesQ = data.gameMoves;
        this.clients = data.playerTable;
        this.outstreams = data.socketOutList;
        this.sem = data.semaphore;
    }

    public void connectToPeer(Integer id, String host, int port) {
        Socket socket = null;
        ObjectOutputStream t_out = null;

        // Save socket out!
        try{
            socket = new Socket(host, port);

            t_out = new ObjectOutputStream(socket.getOutputStream());
            data.addSocketOutToList(id, t_out);
        } catch(Exception e){
            System.err.println("ERROR: Coudn't connect to currently existing client");
        }				    
    }

    public void sendToClient(int client_id, DataPacket packetToClient){
        try{
            ((ObjectOutputStream)outstreams.get(client_id)).writeObject(packetToClient);

            debug("sending packet "+ packetToClient.scenarioType + ", called client " + client_id);
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    public void send(DataPacket packetToClients){
        // Try and get a valid lamport clock!
        DataPacket getClock = new DataPacket();
        packetToClients.lampClk = data.getLamportClock();

	// Get timestamp for report
	Date date = new Date();
	System.out.println("Current timestamp " + date.getTime());


	System.out.println("Client want to send out packet type " + packetToClients.scenarioType);

        int requested_lc;
	if(packetToClients.scenarioType == DataPacket.PLAYER_RESPAWN){
	    try{
	    // Go through each remote client	    
	    	for (ObjectOutputStream out : outstreams.values()) {
	    		out.writeObject(packetToClients);
	    		System.out.println("DISPATCHER: Sending our respawn packets to other clients");
	    	}
	    	chandler.clientRespawnEvent(packetToClients);
	    }catch(Exception e){
            e.printStackTrace();
	    }
	    return;
	} else if(outstreams.size() > 0){
            try{
                // Request a lamport clock if there is more than one client.
		// If packet is for Register, send it to clients right away
                if(packetToClients.scenarioType != DataPacket.PLAYER_REGISTER){
                    while(true){
                        getClock.scenarioType = DataPacket.PLAYER_CLK;
                        requested_lc = data.getLamportClock();
                        getClock.lampClk = requested_lc;
                        getClock.playerID = packetToClients.playerID;

                        // Request awknowledgement from everyone, but yourself
                        // Go through each remote client	    
                        for (ObjectOutputStream out : outstreams.values()) {
                            out.writeObject(getClock);
                            debug("Calling client for clock: " + requested_lc);	    
                        }

                        // Wait until all clients have aknowledged!
                        data.acquireSemaphore(outstreams.size());

                        // You've finally woken up
                        // Check if the lamport clock is valid
                        // If lamport clock is the same as before, it is valid
                        // If it is not, it is invalid and you have to do it all over again
                        if(requested_lc == data.getLamportClock()){
                            break;
                        }		
                    }

                    packetToClients.lampClk = requested_lc;
                    debug("lamport clock before " + data.getLamportClock());

                }

                // Go through each remote client	    
                for (ObjectOutputStream out : outstreams.values()) {
                    out.writeObject(packetToClients);		    
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

	    // Client doesn't have to add a itself again. Exit right away.
            if (packetToClients.scenarioType == DataPacket.PLAYER_SPAWN) {
                data.setClockAndIndex(data.getLamportClock() + 1);
                return;
            }

        } 

	
        if(packetToClients.scenarioType == DataPacket.PLAYER_REGISTER){
            data.acquireSemaphore(outstreams.size());
            return;
        } else if (packetToClients.scenarioType == DataPacket.PLAYER_SPAWN) {	            return;
        } else if (packetToClients.scenarioType == DataPacket.PLAYER_QUIT) {
	    data.acquireSemaphore(outstreams.size());
	    return;
	}

        addEventToOwnQueue(packetToClients);
	
        data.incrementLamportClock();
        debug("lamport clock after " + data.getLamportClock());


	// Get timestamp for report
	System.out.println("After timestamp " + date.getTime());
    }

    private void addEventToOwnQueue(DataPacket packetToSelf) {
        debug("adding own event to queue");
        if (packetToSelf.scenarioType != DataPacket.PLAYER_REGISTER) {
            DataPacket myEvent = new DataPacket();
            myEvent.scenarioType = packetToSelf.scenarioType;
            myEvent.playerName = packetToSelf.playerName;
            myEvent.playerID = packetToSelf.playerID;
            myEvent.lampClk = packetToSelf.lampClk;

            if (packetToSelf.scenarioType == DataPacket.PLAYER_RESPAWN) {
                myEvent.playerFire = packetToSelf.playerFire;
                myEvent.playerDead = packetToSelf.playerDead;
                myEvent.playerLocation = packetToSelf.playerLocation;
                myEvent.playerDirection = packetToSelf.playerDirection;
            }

            chandler.addEventToQueue(myEvent);
            chandler.runEventFromQueue(packetToSelf.lampClk);
        }
    }

    public void debug(String s) {
        if (debug) {
            System.out.println("DISPATCHER: " + s);
        }
    }


}

