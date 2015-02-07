import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

class ServerThread extends Thread {
    private ClientTracker client = null;
    private MainServer server = null;

    public ServerThread(MainServer server, ClientTracker client) {
        this.server = server;
        this.client = client;
    }

    public void run() {
        while (true){
            // Load the packet from a client.
            try {
                DataPacket incoming_packet = (DataPacket)this.client.inStream.readObject();
               
                DataPacket outgoing_packet = new DataPacket();

                switch (incoming_packet.type){
                    case DataPacket.ADD_PLAYER:
                        if(server.currentState == 0){
                            //outgoing_packet = incoming_packet;
                            outgoing_packet.id = this.client.id;
                            outgoing_packet.type = DataPacket.ADD_PLAYER;
                            outgoing_packet.direction = this.client.startingDirection;
                            outgoing_packet.point = this.client.startingPoint;
                            
                            this.client.sendObject(outgoing_packet);
                            outgoing_packet.name = incoming_packet.name;

                            this.client.name = incoming_packet.name;

                            // Send all existing data to the client.
                           
                            for(int i = 0; i < server.action_history.size(); i++){
                                this.client.sendObject(server.action_history.get(i));
                            }
                            outgoing_packet.check = this.server.packetCount++;

                            this.server.bufferQ.add(outgoing_packet);
                        }
                        break;

                   case DataPacket.PLAYER_READY:
                        if(server.currentState == 0){
                            this.client.isReady = true;
                            
                            // Check if all clients are set as ready
                            boolean start_game = true;

                            Iterator<ClientTracker> itr = server.clients.iterator();

                            while(itr.hasNext()){
                                if (itr.next().isReady == false){
                                    start_game = false;
                                }
                            }
                            // If so, notify everyone that the game is beginning
                            if(start_game){
                                //game has started
                                server.currentState = 1;
                                outgoing_packet.id = -1;
                                outgoing_packet.type = DataPacket.START_GAME;
                                 outgoing_packet.check = this.server.packetCount++;
                                this.server.bufferQ.add(outgoing_packet);
                            }
                        }
                    case DataPacket.PLAYER_KILLED:
                        if(server.currentState == server.STATE_PLAYING){
                            // Pick a random starting point, and check to see if it is already occupied
                            outgoing_packet = incoming_packet;
                            outgoing_packet.type = DataPacket.PLAYER_RESPAWN;
                             outgoing_packet.check = this.server.packetCount++;

                            this.server.bufferQ.add(outgoing_packet);
                        }
                        break;
                    default:
                
                }

            } catch (IOException e) {
                // TODO: Add disconnect logic here.
                e.printStackTrace();

                DataPacket disconnect = new DataPacket();
                disconnect.id = this.client.id;
                disconnect.type = DataPacket.REMOVE_PLAYER;
                disconnect.check = this.server.packetCount++;
                this.server.bufferQ.add(disconnect);
                this.server.clients.remove(this.client);

                System.out.println("##################################################");
                System.out.println("Client "+this.client.id+" has disconnected.");
                System.out.println("Clients remaining: "+this.server.clients.size());
                System.out.println("##################################################");

                try {
                    this.client.inStream.close();
                    this.client.outStream.close();
                    this.client.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                this.stop();

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }
}

