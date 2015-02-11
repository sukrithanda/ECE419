import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class ConnectServer {
	/* Variables */
	public ObjectInputStream input;
    public ObjectOutputStream output;
    public Socket socket; 

    /* Send data to server */
    public boolean sendData (DataPacket data) {
        try {
			output.writeObject(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
     
        return true;
    }

    /* Receive data from server */
    public DataPacket receiveData () {
        DataPacket data = null;
        
        try {
            data = (DataPacket)input.readObject();
        } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
        
        return data;
    }
    
    /* Constructor */
    public ConnectServer (int port, String hostname) {
     	try {
	     	this.socket = new Socket(hostname, port);
	     	System.out.println("Before input");
	     	this.input = new ObjectInputStream(socket.getInputStream());
	     	System.out.println("Before output");
	     	this.output = new ObjectOutputStream(socket.getOutputStream());
            this.output.flush();
            System.out.println("After output");
     	} catch (UnknownHostException e) {
     		e.printStackTrace();
     	} catch (IOException e) {
     		e.printStackTrace();
     	} 
    }
}
