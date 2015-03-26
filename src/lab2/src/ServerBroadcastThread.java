import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;

public class ServerBroadcastThread extends Thread {
    MainServer server = null;
    
    public ServerBroadcastThread(MainServer server) {
        this.server = server;
    }

    public void run() {

        while (true){
            
            DataPacket bufferContents[] = server.bufferQ.toArray(new DataPacket[server.bufferQ.size()]);

            server.action_history.addAll(server.bufferQ);
            server.bufferQ.clear();

            Iterator l = server.clients.iterator();
            while (l.hasNext()) {
                ClientTracker client = (ClientTracker)l.next();
                try {
                  for(int i = 0; i <bufferContents.length; i++){
                      client.outputStream.writeObject(bufferContents[i]);
                      System.out.println(">>>"+client.name);
                  }
              } catch (IOException e) {
                  e.printStackTrace();
              }
            }

            if (bufferContents.length != 0) {
                System.out.println("buffer not empty need to send "+ bufferContents.length +" packets from bufferQ to "+ server.clients.size() +" clients.");
            }

            try{
            	//brodcast every 50 milliseconds
                this.sleep(50);
            }
                catch (Exception e){
                    
            }
        }


    }
}
