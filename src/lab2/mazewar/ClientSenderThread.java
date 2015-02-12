
public class ClientSenderThread implements Runnable {

	private ConnectServer connection = null;
	final private GUIClient client;
	
	/* This thread sends data packets in the output queue to the server */
    public void run() {
        for (;;) {
            if (client.output.peek() != null) {
            	DataPacket data = client.output.poll();
                connection.sendData(data);
                System.out.println("CLIENT_SENDER_THREAD: " + data);
            }
        }
    }
    
    public ClientSenderThread (ConnectServer set_connection, GUIClient set_client) {
    	connection = set_connection;
    	client = set_client;
    }
}
