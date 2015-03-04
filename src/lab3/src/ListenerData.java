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

public class ListenerData implements Serializable {

    /**
     * Game state 
     */
    LinkedBlockingQueue<DataPacket> eventQueue = new LinkedBlockingQueue<DataPacket>();
    ConcurrentHashMap<String, DataPacket> clientTable = new ConcurrentHashMap<String, DataPacket>();  // TODO: get rid of this old stuff
    ConcurrentHashMap<Integer, ObjectOutputStream> socketOutList = new ConcurrentHashMap<Integer, ObjectOutputStream>(); 
    ConcurrentHashMap<Integer, DataPacket> lookupTable = new ConcurrentHashMap<Integer, DataPacket>(); // Contains all client data

    /**
     * Client Data
     */
    int myId;
    ConcurrentHashMap<String, Point> clientPosition = new ConcurrentHashMap<String, Point>();

    /**
     * Distributed synchronization resources
     */
    int eventIndex = 0;     // Global lamport clock
    int lamportClock;       // This client's lamport clock
    Semaphore sem = new Semaphore(0); 

    Lock l = new ReentrantLock();

    public void setId(int num){
        myId = num;
    }

    public int getId(){
        return myId; 
    }

    public void acquireSemaphore(int num){
	System.out.println("The amount in sem is " + sem.availablePermits() +  ". Amount required is " + num); 
        try{
            sem.acquire(num);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void releaseSemaphore(int num){
	System.out.println("Releasing " + num + " semaphore(s)");

        try{
            sem.release(num);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addLookupTable(ConcurrentHashMap<Integer, DataPacket> lookupTable){
        this.lookupTable = lookupTable;
    }

    public void addClientToTable(String name, Point position, Direction direction, int type) {
        if (!clientTable.containsKey(name)) {
            /* Create DataPacket */
            DataPacket DataPacket = new DataPacket();
            DataPacket.client_location = position;
            DataPacket.client_direction = direction;
            DataPacket.client_type = type;

            /* Add to table */
            clientTable.put(name, DataPacket);

        } else {
            System.out.println("Client with name " + name + " already exists.");
        }
    }

    public void removeClientFromTable(String name){
        if(clientTable.containsKey(name)){
            clientTable.remove(name);
            clientPosition.remove(name);
        }
    }

    public void addEventToQueue(DataPacket event){
        eventQueue.offer(event);
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
        lamportClock = value;
    }

    public Integer incrementLamportClock() {
        lamportClock++;
	if(lamportClock == 20)
	    lamportClock = 0;
        return lamportClock;
    }

    public Integer getLamportClock(){
	return lamportClock;
    }

    public Integer getEventIndex(){
	return eventIndex;
    }

    public void setEventIndex(Integer i){
	eventIndex = i;
    }


    public void setClockAndIndex(int value){
        lamportClock = value;	
	eventIndex = value;
    }

    public void getLock(){
	l.lock();
    }

    public void releaseLock(){
	l.unlock();
    }

}
