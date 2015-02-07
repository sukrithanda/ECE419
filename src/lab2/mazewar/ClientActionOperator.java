
public class ClientActionOperator implements Runnable {

    final private Maze maze;
    final private Mazewar mazewar;
    final private GUIClient client;

    public ClientActionOperator(Maze maze, Mazewar mazewar, GUIClient client) {
    	this.maze = maze;
        this.mazewar = mazewar;
    	this.client = client;
    }

    private int packetCheck = 0;
    
    @SuppressWarnings("unchecked")
	public void run() {

        while (true){

            if (client.input.peek() == null) {
                continue;
            }

            DataPacket packet = (DataPacket) client.input.poll();

            if (packet.check != -1) {
                assert(packet.check == packetCheck + 1);
                packetCheck = packet.check;
            }

            System.out.println("DEBUG - EXECUTING: " + packet);

            if (packet.type == DataPacket.SET_ID) {
                assert (client.id == -1);
                client.id = packet.id;
                client.addPlayer();
                continue;
            }

            Client player = (Client) client.id_client.get(packet.id);
            assert (player != null);

            switch (packet.type){
            case DataPacket.START_GAME:
                assert(packet.id == client.id);
                client.start_check.compareAndSet(false, true);
                break;
            case DataPacket.LEFT:
                player.turnLeft();
                break;
            case DataPacket.RIGHT:
                player.turnRight();
                break;
            case DataPacket.FORWARD:
                player.forward();
                break;
            case DataPacket.BACKWARD:
                player.backup();
                break;
            case DataPacket.ERROR:
                break;
            case DataPacket.ADD_PLAYER:
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
                Client regen = (Client) client.id_client.get(packet.id);
                assert(regen != null);
                maze.regenClient(regen, packet.direction, packet.point);
            case DataPacket.REMOVE_PLAYER:
                break;
            case DataPacket.FIRE:
                player.fire();
                break;
            default:
                System.out.println("ERROR (NA): " + packet);
                break;
            }
        }

    }
}
