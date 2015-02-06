import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Vector;

class ClientTracker {
    //Static variables since we want to keep track of all clients and their starting points!
    public static int numberofClients = 0;
    public static Vector<Point> startingPoints = new Vector<Point>();
    public static int startingX =0
    public static int startingY=0

    public Socket socket;
    public ObjectOutputStream outStream = null;
    public ObjectInputStream inStream = null;

    public Point startingPoint = null;
    public Direction startingDirection = null;

    public int id;
    public String name = "";
    public boolean isReady = false;

    ClientTracker(Socket socket) throws IOException{

        this.socket = socket;
        this.id = numberofClients;
        numberofClients++;
       
        boolean exists = true;

        while (exists){
            for(int i = 0; i < startingPoints.size(); i++){
                if( (startingPoints.get(i).getX() == startingX && startingPoints.get(i).getY() == startingY){
                    exists = true;
                    startingY +=5;
                    startingX +=2;
                }
                else{
                    exists = false;
                }
            }

        }

        this.startingPoint = new Point(startingX, startingY);
        startingPoints.add(this.startingPoint);
        startingY +=5;
        startingX +=2;

        startingDirection = Direction.random();

        this.outStream = new ObjectOutputStream(this.socket.getOutputStream());
       // this.outStream.flush();
        this.inStream = new ObjectInputStream(this.socket.getInputStream());
    }

    void sendObject(Object obj) throws IOException {
        this.outStream.writeObject(obj);
    }
    	
    }
}