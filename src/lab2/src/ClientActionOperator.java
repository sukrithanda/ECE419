
public class ClientActionOperator implements Runnable {

    final private Maze maze;
    final private Mazewar mazewar;
    final private GUIClient client;

    public ClientActionOperator(Maze maze, Mazewar mazewar, GUIClient client) {
    	this.maze = maze;
        this.mazewar = mazewar;
    	this.client = client;
    }

    @SuppressWarnings("unchecked")
	public void run() {

        while (true){

            if (client.input.peek() != null) {
               
	            DataPacket packet = (DataPacket) client.input.poll();
	            
	            System.out.println("DEBUG - EXECUTING: " + packet.type + "; player id: " + packet.id);

	            /* Inform server of a new player */
	            if (packet.type == DataPacket.SET_ID) {
	                client.id = packet.id;
	                client.addPlayer();
	                continue;
	            }
	
	            /* Get client object from the id_client hashmap that corresponds to the data packet id */
	            Client player = null;
	            try {
	            	player = (Client) client.id_client.get(packet.id);
	            } catch (NullPointerException e) {
	            	e.printStackTrace();
	            }
	
	            /* Case statements to perform operations based upon data packets received from the server */
	            switch (packet.type){
	            case DataPacket.START_GAME:
	            	System.out.println("START_GAME");
	                client.start_check.compareAndSet(false, true);
	                break;
	            case DataPacket.LEFT:
	            	System.out.println("LEFT");
	                player.turnLeft();
	                break;
	            case DataPacket.RIGHT:
	            	System.out.println("RIGHT");
	                player.turnRight();
	                break;
	            case DataPacket.FORWARD:
	            	System.out.println("FORWARD");
	                player.forward();
	                break;
	            case DataPacket.BACKWARD:
	            	System.out.println("BACKWARD");
	                player.backup();
	                break;
	            case DataPacket.ERROR:
	            	System.out.println("ERROR");
	                break;
	            case DataPacket.ADD_PLAYER:
	            	System.out.println("ADD_PLAYER");
	                DirectedPoint position = new DirectedPoint(packet.point, packet.direction);
	                System.out.println("DEBUG - ADDING NEW PLAYER @ " + position + "; id: " + packet.id);
	
	                if (packet.id != client.id){ 
	                    RemoteClient remoteClient = new RemoteClient(packet.name);
	                    remoteClient.id = packet.id;
	                    maze.addClient(remoteClient, position);
	                    client.id_client.put(remoteClient.id, remoteClient);
	                    System.out.println("DEBUG - " + remoteClient.getName() + " ADDED REMOTE CLIENT");
	                } else { 
	                    maze.addClient(client, position);
	                    mazewar.addKeyListener(client);
	                    client.id_client.put(client.id, client);
	                    client.init_check.compareAndSet(false, true);
	                    System.out.println("DEBUG - " + client.getName() + " ADDED LOCAL CLIENT");
	                } 
	                break;
	            case DataPacket.PLAYER_RESPAWN:
	            	System.out.println("PLAYER_RESPAWN");
	                Client regen = (Client) client.id_client.get(packet.id);
	                assert(regen != null);
	                maze.regenClient(regen, packet.direction, packet.point);
	                break;
//	            Commented due to the missile bug issue
//	            case DataPacket.MISSILE_TICK:
//	            	maze.missleTick();
//	            	break;
	            case DataPacket.REMOVE_PLAYER:
	            	System.out.println("REMOVE_PLAYER");
	                break;
	            case DataPacket.FIRE:
	            	System.out.println("FIRE");
	                player.fire();
	                break;
	            default:
	                System.out.println("ERROR (NA): " + packet);
	                break;
	            }
	        }
        }
    }
}
