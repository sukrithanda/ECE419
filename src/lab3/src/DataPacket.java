import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;


public class DataPacket implements Serializable {
    
	// Player connection info
	public int portNum;
    public String hostName;
    
	// Player info
	Client player;
    public int playerID;
    public int playerType;
    public String playerName;
    public Direction playerDirection;
    public Point playerLocation;
    public int playerScore; 

    // Info about game state
    public boolean newPlayer = false;
    public int errType;
    public int scenarioType;
    public int orderVal;
    
    // Info about other players and NameServer
    ConcurrentHashMap<String, DataPacket> playerList;
    ConcurrentHashMap<Integer, DataPacket> NSTable;
    
    // Info oabout logical (Lamport) clk
    int lampClk;
    boolean clkCredible;   
    
    // Missile fire logistic
    public Integer playerFire; 
    public Integer playerDead; 
    
    // Error codes
    public static final byte ERR_RESERVED_NS_PORT = 4;
    public static final byte ERR_SECURED_LOCATION = 5;
    
    // Player types
    public static final byte PLAYER_ROBOT = 6;
    public static final byte PLAYER_REMOTE = 7;
    
    // NameServer (NS) constants
    public static final byte NS_RESPONSE = 8;
    public static final byte NS_REGISTER = 9;
    public static final byte NS_QUIT = 10;
    
    // Client side game scenarios
    public static final byte PLAYER_REGISTER = 11;
    public static final byte PLAYER_QUIT = 12;
    public static final byte PLAYER_ACK = 13;
    public static final byte PLAYER_CLK = 14;
    public static final byte PLAYER_SPAWN = 15;
    public static final byte PLAYER_RESPAWN = 16;
    public static final byte PLAYER_FIRE = 17;
    public static final byte PLAYER_FORWARD = 18;
    public static final byte PLAYER_RIGHT = 19;
    public static final byte PLAYER_LEFT = 20;
    public static final byte PLAYER_BACK = 21;
    public static final byte PLAYER_ORDER_VAL = 22;
    public static final byte PLAYER_FREE_SEMAPHORE = 23;
    public static final byte PLAYER_SECURE_LOCATION = 24;
    
}
