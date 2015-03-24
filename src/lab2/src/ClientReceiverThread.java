
public class ClientReceiverThread implements Runnable {

	private ConnectServer connection = null;
	final private GUIClient client;

	/* This thread receives data packets from the server, and adds
	 * them into the input queue */
    public void run() {
       for(;;) {
            DataPacket data = (DataPacket) connection.receiveData();
            client.input.add(data);
            System.out.println("CLIENT_RECEIVER_THREAD: " + data.type);
        }
    }
    
    public ClientReceiverThread (ConnectServer set_connection, GUIClient set_client) {
    	connection = set_connection;
    	client = set_client;
    }
}
