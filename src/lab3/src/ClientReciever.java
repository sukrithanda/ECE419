import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;
import java.net.*;

/*
 * ClientReceiver - Receive DataPackets from all clients
 */

public class ClientReciever extends Thread{

	int portClient;
	DataPacketManager listenerData;
    Broadcaster broadcaster;
    ClientHandlerThread clientHandler;

    public void run(){
    	try{
			ServerSocket receiver_socket = new ServerSocket(portClient);
			while (true) {
				new ClientRecieverThread(clientHandler, receiver_socket.accept(), listenerData, broadcaster).start();	    
			}
        } catch (IOException e) {
            System.err.println("ERROR: Unable to establish connection on client Port " + portClient);
            System.exit(-1);
        }
    }
    
    public ClientReciever(ClientHandlerThread clientHandler, int portClient, 
    		DataPacketManager listenerData, Broadcaster broadcaster){
		this.portClient = portClient;
		this.listenerData = listenerData;
		this.broadcaster = broadcaster;
		this.clientHandler = clientHandler;            
	}
}

