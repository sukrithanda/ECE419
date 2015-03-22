import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DataPacketManager - Provides synchronization support for handling data packets
 */

public class DataPacketManager implements Serializable {

    /* Synchronization Support */
    Lock lock = new ReentrantLock();
    Semaphore semaphore = new Semaphore(0); 
    
    int orderVal = 0;	
    int lampClk;

	/* Local Client Information */
    int playerID;
    ConcurrentHashMap<String, Point> playerPositions = new ConcurrentHashMap<String, Point>();
    
    /* Global Game Information */
    ConcurrentHashMap<Integer, ObjectOutputStream> outputMessageStreams = new ConcurrentHashMap<Integer, ObjectOutputStream>(); 
    LinkedBlockingQueue<DataPacket> playerOps = new LinkedBlockingQueue<DataPacket>();
    
    int test = 1;

    public void print(String str) {
        if (test == 1) {
            System.out.println("DEBUG: (DataPacketManager) " + str);
        }
    }
    
    /* Record/Verify player operation */
    public boolean recordPlayerOp(Point position, String name) { 
        if(playerPositions.containsValue(position)){
            Point playerPosition = playerPositions.get(name);
            if(playerPosition == position) {
                return true;
            } else {
                return false;
            }
        }else{
        	playerPositions.put(name,position);	  	    
            return true;
        }
    }
    
    /* Add outputMessageStreams */
    public void addOutputStream(ObjectOutputStream outStream, Integer key) {
        outputMessageStreams.put(key, outStream);
    }

    /* Delete from outputMessageStreams */
    public void deleteOutputStream(Integer key) {
        outputMessageStreams.remove(key);
    }

    /* Getter for operation order */
    public Integer getOpOrder(){
    	return orderVal;
    }

    /* Setter for operation order */
    public void setOpOrder(Integer order){
    	orderVal = order;
    }

    /* Setter for Lamport clock and operation order */
    public void setLampClkIndex(int val){
    	setOpOrder(val);
    	initLampClk(val);
    }

    /* Method to acquire lock */
    public void acqLock(){
    	lock.lock();
    }

    /* Method to free lock */
    public void freeLock(){
    	lock.unlock();
    }
    
    /* Method to acquire semaphore */
    public void acqSemaphore(int order){
        try{
            semaphore.acquire(order);
        } catch (Exception e){
            e.printStackTrace();
        }
    	print("SEMAPHORE acquired - " + semaphore.availablePermits() +  "; Needed - " + order); 
    }

    /* Method to free semaphore */
    public void freeSemaphore(int order){
        try{
            semaphore.release(order);
        } catch (Exception e){
            e.printStackTrace();
        }
    	print("SEMAPHORE freed - " + order + " semaphore(s)");
    }
    
    /* Setter for player ID */
    public void setPlayerID(int id){
        playerID = id;
    }

    /* Getter for player ID */
    public int getPlayerID(){
        return playerID; 
    }
    
    /* Lamport clock tick */
    public Integer incrLampClk() {
        lampClk++;
        if(lampClk == 25)
        	lampClk = 0;
        return lampClk;
    }

    /* Initialize Lamport clock */
    public void initLampClk(int val){
        lampClk = val;
    }

    /* Getter for Lamport clock */
    public Integer getLampClk(){
    	return lampClk;
    }
}
