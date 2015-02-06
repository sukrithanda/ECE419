import java.io.Serializable;

public class DataPacket implements Serializable {
	/* Constants */
    public static final int START = 11;
    public static final int READY = 12;
    public static final int ERROR = 13;
    public static final int KILLED = 14;
    public static final int RESPAWN = 15;
    public static final int GET_ID = 16;
    public static final int SET_ID = 17;
    
	public static final int ADD = 21;
    public static final int REMOVE = 22;
    public static final int SHOOT = 23;
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
    	case START:
    		status = "START";
    		break;
    	case READY:
    		status = "READY";
    		break;
    	case ERROR:
    		status = "ERROR";
    		break;
    	case KILLED:
    		status = "KILLED";
    		break;
    	case RESPAWN:
    		status = "RESPAWN";
    		break;
    	case GET_ID:
    		status = "GET_ID";
    		break;
    	case SET_ID:
    		status = "SET_ID";
    		break;
    	case ADD:
    		status = "ADD";
    		break;
    	case REMOVE:
    		status = "REMOVE";
    		break;
    	case SHOOT:
    		status = "SHOOT";
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
