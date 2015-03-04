import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/* MazePacket

   Contains basic variables that client and server shall use to communicate with each other.
   */


public class DataPacket implements Serializable {


    /**
	 * 
	 */
	// Actions
    public static final byte SERVER_ACK = 10; 
    public static final byte SERVER_CLIENT_LIST = 11;
    public static final byte SERVER_EVENT_LIST = 12;

    /**
     * Client sends packet with name, position (?)
     * Server adds to event queue with client list included
     */
    public static final byte CLIENT_REGISTER = 21; // Client wants to register! IP of client shall be passed in.
    public static final byte CLIENT_FORWARD = 22;
    public static final byte CLIENT_BACK = 23;
    public static final byte CLIENT_LEFT = 24;
    public static final byte CLIENT_RIGHT = 25;
    public static final byte CLIENT_FIRE = 26;
    public static final byte CLIENT_RESPAWN = 27;
    public static final byte CLIENT_QUIT = 28;
    public static final byte CLIENT_ACK = 29;
    public static final byte CLIENT_CLOCK = 30;
    public static final byte CLIENT_SPAWN = 31;
    public static final byte CLIENT_REL_SEM = 32;

    // Lookup service
    public static final byte LOOKUP_REPLY = 33;
    public static final byte LOOKUP_REGISTER = 34;
    public static final byte LOOKUP_QUIT = 35;
    public static final byte LOOKUP_UPDATE = 36;
    public static final byte LOOKUP_GET = 37;

    // Misc.
    public static final byte RESERVE_POINT = 41;
    public static final byte GET_SEQ_NUM = 42;

    // Error code
    public static final byte ERROR_INVALID_ARG = -11;
    public static final byte ERROR_RESERVED_POSITION = -12;
    public static final byte ERROR_LOOKUP_PORT = -13;

    // Client type
    public static final byte ROBOT = 1;
    public static final byte REMOTE = 2;

    // Client actions
    public String client_host;
    public int client_port;
    
    Client c;

    public int client_id;
    public String client_name;
    public Point client_location;
    public Direction client_direction;
    public int client_type;
    public int client_score; 

    public boolean for_new_client = false;

    // Client shot
    public Integer shooter; // Source / killer
    public Integer target; // Targer / victim

    //Server actions
    int ack_num;

    // Event
    public MazeEvent event;

    // Game data
    // Contains all client information within Client data
    ConcurrentHashMap<String, DataPacket> client_list;

    ConcurrentHashMap<Integer, DataPacket> NameServerTable;
    // Packet data
    int sequence_num;
    int packet_type;
    public int error_code;

    // Lamport clock
    int lamportClock;
    boolean isValidClock;   


}
