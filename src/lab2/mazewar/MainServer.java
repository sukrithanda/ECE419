import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class MainServer{
	//using vectors because they are thread safe
	public Vector<ClientTracker> clients = new Vector<ClientTracker>();
	public Vector<DataPacket> action_history = new Vector<DataPacket>();
	public static ServerSocket serverSocket = null;
	
	//thread safe FIFO queue
    public Queue<DataPacket> bufferQ = new ConcurrentLinkedQueue<DataPacket>();
    public static int packetCount = 1;
    public final int STATE_PLAYING = 1;
    public static int currentState = 0;

    public static void main(String args[]) throws IOException {
        MainServer server = new MainServer();

        try {
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.out.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
	       ServerBroadcastThread BroadcastThread = new ServerBroadcastThread(server);
	       BroadcastThread.start();

	
        while(true){
            System.out.println("waiting");

            Socket incoming = serverSocket.accept();
            System.out.println("New player detected");

            ClientTracker createPlayer = new ClientTracker(incoming);
            server.clients.add(createPlayer);
            ServerThread clientHandler = new ServerThread(server, createPlayer);
            clientHandler.start();

        }
    }
}
