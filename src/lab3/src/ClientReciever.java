import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;
import java.net.*;

/* MazewarListener class
 *
 * Recieve all client event packets
 *
 */
public class ClientReciever extends Thread{

    Broadcaster bd;
    ListenerData data;
    ClientHandlerThread handler;
    int client_port;

    public ClientReciever(int client_port, ListenerData data, Broadcaster bd, ClientHandlerThread handler){
	this.client_port = client_port;
	this.bd = bd;
	this.data = data;
	this.handler = handler;            }

    public void run(){
        //ServerSocket receiver_socket = null;
	try{
			ServerSocket receiver_socket = new ServerSocket(client_port);

	    /* Listen for new remote clients */
			while (true) {
				new ClientRecieverThread(receiver_socket.accept(), data, bd, handler).start();	    
			}
  
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
    }
}

