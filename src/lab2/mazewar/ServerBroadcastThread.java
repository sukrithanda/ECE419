import java.io.IOException;
import java.util.Enumeration;
import java.util.Random;

public class ServerBroadcastThread extends Thread {
    //int tickRate;
    MainServer server = null;

    public ServerBroadcastThread(MainServer server) {
        this.server = server;
    }

    public void run() {
        //Clear packets from the bufferQ n times per second.
        //long timeBetweenTicks = 1000/tickRate;

        while (true){
            
            // Build a serialiazable packet
            DataPacket bufferContents[] = server.bufferQ.toArray(new DataPacket[server.bufferQ.size()]);

            // Backup the actions and send clear the buffer.
            server.action_history.addAll(server.bufferQ);
            server.bufferQ.clear();

            Enumeration enum_clients = server.clients.elements();
            while (enum_clients.hasMoreElements()){
                ClientTracker client = (ClientTracker)enum_clients.nextElement();

                try {
                    for(int i = 0; i <bufferContents.length; i++){
                        client.sendObject(bufferContents[i]);
                        System.out.println(">>>"+client.name);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bufferContents.length != 0) {
                System.out.println("Sending "+ bufferContents.length +" packets from buffer to "+ server.clients.size() +" clients.");
            }

            try{
                this.sleep(50);
            }
                catch (Exception e){
                    
            }
        }


    }
}
