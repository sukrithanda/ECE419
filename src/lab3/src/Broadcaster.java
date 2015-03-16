import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broadcaster - Handles message sending to peers
 */

public class Broadcaster extends Thread {

    public Broadcaster(DataPacketManager manager, MazewarP2PHandler peerHandler) {
        this.outgoing = manager.outputMessageStreams;
        this.peerHandler = peerHandler;
        this.manager = manager;
    }

    DataPacketManager manager;
    MazewarP2PHandler peerHandler;

    ConcurrentHashMap<Integer, ObjectOutputStream> outgoing = new ConcurrentHashMap<Integer, ObjectOutputStream>(); 
    
    boolean debug = true;

    public void print(String str) {
        if (debug) {
            System.out.println("DEBUG: (Broadcaster) " + str);
        }
    }
    
    /* Establish connection with peer */
    public void peerConnection(String hostName, int portNum, Integer id) { 
        Socket socket = null;
        try{
            socket = new Socket(hostName, portNum);
            manager.addOutputStream(new ObjectOutputStream(socket.getOutputStream()), id);
        } catch(Exception e){
        	print("Unable to connect peer " + hostName);
        }				    
    }
    
    /* Store operation for local player */
    private void recordPlayerOperation(DataPacket dpIn) { 
        if (dpIn.scenarioType != DataPacket.PLAYER_REGISTER) {
            DataPacket dpOut = new DataPacket();
            
            if (dpIn.scenarioType == DataPacket.PLAYER_RESPAWN) {
                dpOut.playerFire = dpIn.playerFire;
                dpOut.playerLocation = dpIn.playerLocation;
                dpOut.playerDirection = dpIn.playerDirection;
                dpOut.playerDead = dpIn.playerDead;
            }
            
            dpOut.scenarioType = dpIn.scenarioType;
            dpOut.playerID = dpIn.playerID;
            dpOut.playerName = dpIn.playerName;
            dpOut.lampClk = dpIn.lampClk;

            peerHandler.recordPlayerOp(dpOut);
            peerHandler.executePlayerOperation(dpIn.lampClk);
        }
    }

    /* Send message to specific peer */
    public void peerSend(DataPacket dp, int id){ 
        try{
        	print("Client ID - " + id + "Packet Type - " + dp.scenarioType);
            ((ObjectOutputStream)outgoing.get(id)).writeObject(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    /* Multicast message to all peers */
	public void peerSendMulticast(DataPacket dp) {
		print("Packet Type - " + dp.scenarioType);
		
		int curLampClk;
		DataPacket dpClk = new DataPacket();
		dp.lampClk = manager.getLampClk();

		if (dp.scenarioType == DataPacket.PLAYER_RESPAWN) {
			try {
				for (ObjectOutputStream out : outgoing.values()) {
					print("Send RESPAWN message to peers");
					out.writeObject(dp);
				}
				peerHandler.playerRespawn(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		} else if (outgoing.size() > 0) {
			try {
				if (dp.scenarioType != DataPacket.PLAYER_REGISTER) {
					while (true) {
						curLampClk = manager.getLampClk();
						dpClk.scenarioType = DataPacket.PLAYER_CLK;
						dpClk.lampClk = curLampClk;
						dpClk.playerID = dp.playerID;

						for (ObjectOutputStream out : outgoing.values()) {
							print("Current Lamport Clock " + curLampClk);
							out.writeObject(dpClk); /* Ask players to ACK */
						} 

						manager.acqSemaphore(outgoing.size()); /* Ask players to ACK */

						if (curLampClk == manager.getLampClk()) { /* Try again if incorrect Lamport clock */
							break;
						}
					}

					dp.lampClk = curLampClk;
					print("LCLK (START) " + manager.getLampClk());
				}

				for (ObjectOutputStream out : outgoing.values()) {
					out.writeObject(dp);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			if (dp.scenarioType == DataPacket.PLAYER_SPAWN) {
				manager.setLampClkIndex(manager.getLampClk() + 1);
				return;
			}

		}

		if (dp.scenarioType == DataPacket.PLAYER_REGISTER) {
			manager.acqSemaphore(outgoing.size());
			return;
		} else if (dp.scenarioType == DataPacket.PLAYER_SPAWN) {
			return;
		} else if (dp.scenarioType == DataPacket.PLAYER_QUIT) {
			manager.acqSemaphore(outgoing.size());
			return;
		}

		recordPlayerOperation(dp);
		manager.incrLampClk();
		print("LCLK (End) " + manager.getLampClk());
	}
}

