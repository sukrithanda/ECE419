import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;
import java.util.concurrent.Semaphore;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataPacketManager implements Serializable {

	/* Local Client Information */
    int clientId;
    ConcurrentHashMap<String, Point> clientPosition = new ConcurrentHashMap<String, Point>();
    
    /* Global Game Information */
    LinkedBlockingQueue<DataPacket> gameMoves = new LinkedBlockingQueue<DataPacket>();
    ConcurrentHashMap<String, DataPacket> playerTable = new ConcurrentHashMap<String, DataPacket>();  // TODO: get rid of this old stuff
    ConcurrentHashMap<Integer, ObjectOutputStream> socketOutList = new ConcurrentHashMap<Integer, ObjectOutputStream>(); 
    ConcurrentHashMap<Integer, DataPacket> lookupTable = new ConcurrentHashMap<Integer, DataPacket>(); // Contains all client data

    int moveIndex = 0;	
    int LCLK;       // Lamport clock
    
    Lock lock = new ReentrantLock();
    Semaphore semaphore = new Semaphore(0); 

    boolean DEBUG = true;
    
    public void setId(int id){
        clientId = id;
    }

    public int getId(){
        return clientId; 
    }

    public void addLookupTable(ConcurrentHashMap<Integer, DataPacket> lookupTable){
        this.lookupTable = lookupTable;
    }

    public void addClientToTable(String name, Point position, Direction direction, int type) {
        if (playerTable.containsKey(name)) {
        	print("Player, " + name + ", being re-added.");
        } else {
            DataPacket dp = new DataPacket(); /* Create */
            dp.client_location = position;
            dp.client_direction = direction;
            dp.client_type = type;

            playerTable.put(name, dp);	/* Insert */
        }
    }

    public void removeClientFromTable(String name){
        if(playerTable.containsKey(name)){
            playerTable.remove(name);
            clientPosition.remove(name);
        }
    }

    public void recordMove(DataPacket event){
        gameMoves.offer(event);
    }

    public void addSocketOutToList(Integer key, ObjectOutputStream out) {
        socketOutList.put(key, out);
    }

    public void removeSocketOutFromList(Integer key) {
        socketOutList.remove(key);
    }

    public boolean setPosition(String name, Point position){
        if(!clientPosition.containsValue(position)){
            clientPosition.put(name,position);	  	    
            return true;
        }else{
            Point clientPos = clientPosition.get(name);
            if(clientPos == position)
                return true;
            else
                return false;
        }
    }

    public void setLamportClock(int value){
        LCLK = value;
    }

    public Integer incrementLamportClock() {
        LCLK++;
        if(LCLK == 20)
        	LCLK = 0;
        return LCLK;
    }

    public Integer getLamportClock(){
    	return LCLK;
    }

    public Integer getMoveIndex(){
    	return moveIndex;
    }

    public void setMoveIndex(Integer i){
    	moveIndex = i;
    }


    public void setClockAndIndex(int value){
        LCLK = value;	
        moveIndex = value;
    }

    public void acquireSemaphore(int index){
    	print("SEMAPHORE: Acquired - " + semaphore.availablePermits() +  "; Needed - " + index); 
        try{
            semaphore.acquire(index);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void freeSemaphore(int index){
    	print("SEMAPHORE: Free " + index + " semaphore(s)");
        try{
            semaphore.release(index);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public void acquireLock(){
    	lock.lock();
    }

    public void freeLock(){
    	lock.unlock();
    }

    //BABNEET - REMOVE
    private void print(String s) {
        if (DEBUG) {
            System.out.println("DEBUG: (DataPacketManager) " + s);
        }
    }
}
