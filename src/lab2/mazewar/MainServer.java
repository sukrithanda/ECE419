import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class MainServer{
	//using vectors because they are thread safe
	public Vector<ClientTracker> clients = new Vector<ClientTracker>();
	public Vector<DataPacket> action_history = new Vector<DataPacket>();

	//thread safe FIFO queue
  //  public Queue<DataPacket> incomingQ = new ConcurrentLinkedQueue<DataPacket>();
    public Queue<DataPacket> bufferQ = new ConcurrentLinkedQueue<DataPacket>();
    public static int packetCount = 1;
    public final int STATE_PLAYING = 1;
    public static int currentState = 0;

    public static void main(String args[]) throws IOException {
        MainServer server = new MainServer();

        try {
		//System.out.print("arg length "+args.length);
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);

       //MainServerThread tickThread = new MainServerThread(server, 20);
       // tickThread.start();

       // MazewarServerProcessThread processThread = new MazewarServerProcessThread(server);
       // processThread.start();


        while(true){
            //Listen for incoming connections, and create new sockets.
            System.out.println("Waiting for new connection...");
            Socket incoming = serverSocket.accept();
            System.out.println("New client accepted.");

         //   if(server.gameState != server.STATE_SETUP){
                // Reject client, since game is in progress.
           // }else{
                ClientTracker createPlayer = new ClientTracker(incoming);
                server.clients.add(createPlayer);

                System.out.println("Spawning new thread.");

                ServerThread clientHandler = new ServerThread(server, createPlayer);
                clientHandler.start();
          //  }
        }
    }
}
