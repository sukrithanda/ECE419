import java.io.IOException;
import java.net.ServerSocket;

/*
 * ClientReceiver - Receive DataPackets from all clients
 */

public class ClientReciever extends Thread{

	int clientSocketPort;
	DataPacketManager manager;
    Broadcaster broadcaster;
    ClientHandlerThread clientHandler;
    public ClientReciever(ClientHandlerThread clientHandler, int portClient, DataPacketManager listenerData, Broadcaster broadcaster){
		this.clientSocketPort = portClient;
		this.manager = listenerData;
		this.broadcaster = broadcaster;
		this.clientHandler = clientHandler;            
	}
    
    public void run(){
    	try{
			ServerSocket receiver_socket = new ServerSocket(clientSocketPort);
			while (true) {
				new ClientRecieverThread(clientHandler, receiver_socket.accept(), manager, broadcaster).start();	    
			}
        } catch (IOException e) {
            System.err.println("ERROR: Unable to establish connection on client Port " + clientSocketPort);
            System.exit(-1);
        }
    }
    
   
}

