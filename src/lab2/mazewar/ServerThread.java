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
                DataPacket incoming_packet = (DataPacket)this.client.inputStream.readObject();
               
                DataPacket outgoing_packet = new DataPacket();

                if (incoming_packet.type == DataPacket.GET_ID){
                    if(server.currentState == 0){
                    	//set the starting point and the id
                        outgoing_packet.id = this.client.id;
                        outgoing_packet.type = DataPacket.SET_ID;
                        outgoing_packet.direction = this.client.startingDirection;
                        outgoing_packet.point = this.client.startingPoint;
                        
                        this.client.outputStream.writeObject(outgoing_packet);
                        // Send all existing data to the client.
                        for(int i = 0; i < server.action_history.size(); i++){
                            this.client.outputStream.writeObject(server.action_history.get(i));
                        }
                    }
                }
                if (incoming_packet.type == DataPacket.PLAYER_READY){
                    if(server.currentState == 0){
                        this.client.isReady = true;
                        System.out.println("client is now ready");
                        
                        boolean clients_ready = true;
                        Iterator i = server.clients.iterator();
                        while (i.hasNext()) {
                            ClientTracker client = (ClientTracker)i.next();
                        	 if(client.isReady == false){
                        		 clients_ready = false;
                                 break;
                             }
                        }

                        if(clients_ready){
                            System.out.println("##"+incoming_packet.id+": ALL CLIENTS READY. START GAME");
                            server.currentState = 1;
                            incoming_packet = new DataPacket();
                            incoming_packet.id = -1;
                            incoming_packet.type = DataPacket.START_GAME;
                            addIncr(incoming_packet);
                        }
                    }
                }
      
                if (incoming_packet.type == DataPacket.ADD_PLAYER){

                    if(server.currentState == 0){
                        outgoing_packet.id = this.client.id;
                        //retreive new starting point and direction when client was created
                        outgoing_packet.direction = this.client.startingDirection;
                        outgoing_packet.point = this.client.startingPoint;
                        outgoing_packet.type = DataPacket.ADD_PLAYER;
                        outgoing_packet.name = incoming_packet.name;

                        this.client.name = incoming_packet.name;
                        addIncr(outgoing_packet);
                    }
                }
                if (incoming_packet.type == DataPacket.PLAYER_KILLED){
               // case DataPacket.PLAYER_KILLED:
                    if(server.currentState == 1){
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
                }
                else { 
                    if(server.currentState == 1){
                        System.out.println("##"+incoming_packet.id+": To the buffer.");
                        outgoing_packet.id = incoming_packet.id;
                        outgoing_packet.type = incoming_packet.type;
                        addIncr(outgoing_packet);
                    }
                }
            }

             catch (IOException e) {
                e.printStackTrace();

                DataPacket disconnect = new DataPacket();
                disconnect.id = this.client.id;
                disconnect.type = DataPacket.REMOVE_PLAYER;
                disconnect.check = this.server.packetCount++;
                addIncr(disconnect);
                this.server.clients.remove(this.client);

                System.out.println("Client "+this.client.id+" has disconnected.");
                try {
                    this.client.inputStream.close();
                    this.client.outputStream.close();
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

