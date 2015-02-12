import java.io.Serializable;

public class DataPacket implements Serializable {
    /* Constants to determine possible game scenarios */
    public static final byte START_GAME = 11;
    public static final byte PLAYER_READY = 12;
    public static final byte ERROR = 13;
    public static final byte PLAYER_KILLED = 14;
    public static final byte PLAYER_RESPAWN = 15;
    public static final byte GET_ID = 16;
    public static final byte SET_ID = 17;
    
    public static final byte ADD_PLAYER = 21;
    public static final byte REMOVE_PLAYER = 22;
    public static final byte FIRE = 23;
    public static final byte LEFT = 24;
    public static final byte RIGHT = 25;
    public static final byte FORWARD = 26;
    public static final byte BACKWARD = 27;

    /* Variables */
    public byte id = 0;
    public byte type = 13;
    public int check = Integer.MIN_VALUE;
    String name = null;
    Point point;
    Direction direction;
}
