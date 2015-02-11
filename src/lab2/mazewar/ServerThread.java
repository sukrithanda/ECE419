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
            // Load the incoming_packet from a client.
            try {
                DataPacket incoming_packet = (DataPacket)this.client.inStream.readObject();
               
                DataPacket outgoing_packet = new DataPacket();

                switch (incoming_packet.type){
                case DataPacket.GET_ID:
                    if(server.currentState == 0){
                        outgoing_packet.id = this.client.id;
                        outgoing_packet.type = DataPacket.SET_ID;
                        outgoing_packet.direction = this.client.startingDirection;
                        outgoing_packet.point = this.client.startingPoint;
                        // Set a random location.
                        System.out.println("##"+outgoing_packet.id+": Returning client incoming_packet");
                        this.client.sendObject(outgoing_packet);

                        // Send all existing data to the client.
                        System.out.println("Catching up client. Syncing "+server.action_history.size()+" previous actions.");
                        for(int i = 0; i < server.action_history.size(); i++){
                            this.client.sendObject(server.action_history.get(i));
                        }
                    }
                    break;
                case DataPacket.SET_ID:
                    System.out.println("##"+incoming_packet.id+": Error - received a SET incoming_packet from this client.");
                    break;
                case DataPacket.PLAYER_READY:
                    if(server.currentState == 0){
                        this.client.isReady = true;
                        System.out.println("##"+incoming_packet.id+": Setting client as ready");

                        // Check if all clients are set as ready
                        boolean all_ready = true;
                        Enumeration enum_clients = server.clients.elements();
                        while (enum_clients.hasMoreElements()){
                            ClientTracker client = (ClientTracker)enum_clients.nextElement();
                            if(client.isReady == false){
                                all_ready = false;
                            }
                        }

                        // If so, notify everyone that the game is beginning
                        if(all_ready){
                            server.currentState = server.STATE_PLAYING;
                            incoming_packet = new DataPacket();
                            incoming_packet.id = -1;
                            incoming_packet.type = DataPacket.START_GAME;
                            addIncr(incoming_packet);
                        }
                    }
                    break;
                case DataPacket.ADD_PLAYER:
                    if(server.currentState == 0){
                        // Pick a random starting point, and check to see if it is already occupied
                        outgoing_packet.id = this.client.id;
                        outgoing_packet.direction = this.client.startingDirection;
                        outgoing_packet.point = this.client.startingPoint;
                        outgoing_packet.type = DataPacket.ADD_PLAYER;
                        outgoing_packet.name = incoming_packet.name;

                        this.client.name = incoming_packet.name;

                        // Set a random location.
                        System.out.println("##"+incoming_packet.id+": To the buffer. Adding new client to maze for all players.");
                        addIncr(outgoing_packet);
                    }
                    break;
                case DataPacket.PLAYER_KILLED:
                    if(server.currentState == server.STATE_PLAYING){
                        // Pick a random starting point, and check to see if it is already occupied
                        outgoing_packet.id = incoming_packet.id;
                        outgoing_packet.direction = incoming_packet.direction;
                        outgoing_packet.point = incoming_packet.point;
                        outgoing_packet.type = DataPacket.PLAYER_RESPAWN;
                        outgoing_packet.name = incoming_packet.name;

                        // Set a random location.
                        System.out.println("##"+incoming_packet.id+": To the buffer. Sending respawn message to everyone.");
                        addIncr(outgoing_packet);
                        
                    }
                    break;
                default:
                    if(server.currentState == server.STATE_PLAYING){
                        System.out.println("##"+incoming_packet.id+": To the buffer.");
                        outgoing_packet.id = incoming_packet.id;
                        outgoing_packet.type = incoming_packet.type;
                        addIncr(outgoing_packet);
                    }
            }

            } catch (IOException e) {
                // TODO: Add disconnect logic here.
                e.printStackTrace();

                DataPacket disconnect = new DataPacket();
                disconnect.id = this.client.id;
                disconnect.type = DataPacket.REMOVE_PLAYER;
                disconnect.check = this.server.packetCount++;
                addIncr(disconnect);
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
    
    public void addIncr (DataPacket outgoing_packet) {
    	this.server.packetCount++;
    	this.server.bufferQ.add(outgoing_packet);
    }
}

