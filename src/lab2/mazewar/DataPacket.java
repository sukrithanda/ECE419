import java.io.Serializable;

public class DataPacket implements Serializable {
    /* Constants */
    public static final int START_GAME = 11;
    public static final int PLAYER_READY = 12;
    public static final int ERROR = 13;
    public static final int PLAYER_KILLED = 14;
    public static final int PLAYER_RESPAWN = 15;
    public static final int GET_ID = 16;
    public static final int SET_ID = 17;
    
    public static final int ADD_PLAYER = 21;
    public static final int REMOVE_PLAYER = 22;
    public static final int FIRE = 23;
    public static final int LEFT = 24;
    public static final int RIGHT = 25;
    public static final int FORWARD = 26;
    public static final int BACKWARD = 27;

    /* Variables */
    int id = 0;
    int type = 13;
    int check = Integer.MIN_VALUE;
    String name = null;
    Point point;
    Direction direction;

    /* Provide summary of packet content */
    public String getSummary () {
        String status = "";
        
        switch (type) {
        case START_GAME:
            status = "START";
            break;
        case PLAYER_READY:
            status = "READY";
            break;
        case ERROR:
            status = "ERROR";
            break;
        case PLAYER_KILLED:
            status = "KILLED";
            break;
        case PLAYER_RESPAWN:
            status = "RESPAWN";
            break;
        case GET_ID:
            status = "GET_ID";
            break;
        case SET_ID:
            status = "SET_ID";
            break;
        case ADD_PLAYER:
            status = "ADD_PLAYER";
            break;
        case REMOVE_PLAYER:
            status = "REMOVE_PLAYER";
            break;
        case FIRE:
            status = "FIRE";
            break;
        case LEFT:
            status = "LEFT";
            break;
        case RIGHT:
            status = "RIGHT";
            break;
        case FORWARD:
            status = "FORWARD";
            break;
        case BACKWARD:
            status = "BACKWARD";
            break;
        default:
            status = "UNKNOWN";
        }
        
        return "DEBUG: id = " + id + "; check = " + "; status = " + status + "\n";
    }
}
