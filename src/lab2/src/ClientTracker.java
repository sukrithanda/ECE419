import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Vector;

class ClientTracker {
    //Static variables since we want to keep track of all clients and their starting points!
    public static byte numberofClients;
    public static Vector<Point> startingPoints = new Vector<Point>();
    public static int startingX = 0;
    public static int startingY= 0;

    public Socket socket;
    public ObjectOutputStream outputStream = null;
    public ObjectInputStream inputStream = null;

    public Point startingPoint = null;
    public Direction startingDirection = null;

    public byte id;
    public String name = "";
    public boolean isReady = false;
    public Random number_gen = new Random();

    ClientTracker(Socket socket) throws IOException{

        this.socket = socket;
        this.id = numberofClients;
        numberofClients++;
               this.setstartingpoint();

        startingDirection = Direction.random();

        this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(this.socket.getInputStream());
    }

   // void sendObject(Object obj) throws IOException {
     //   this.outputStream.writeObject(obj);
  //  }
    
    void setstartingpoint() throws IOException {
        while(true){
            // generate random x, y starting points
            startingY = number_gen.nextInt(10);
            startingX = number_gen.nextInt(20);
            //check if they already exists
            boolean used = false;
            for(int i = 0; i < startingPoints.size(); i++){
                if(startingPoints.get(i).getX() == startingX && startingPoints.get(i).getY() == startingY){
                    used = true;
                    break;
                }
            }
            if(used == false){
                this.startingPoint = new Point(startingX, startingY);
                startingPoints.add(this.startingPoint);
                return;
            }
        }

    }
    	
}
