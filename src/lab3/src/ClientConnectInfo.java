import java.io.Serializable;

public class ClientConnectInfo implements Serializable {
    public String host;
    public int port;

    public ClientConnectInfo(int port, String host){
        this.host = host;
        this.port = port;
    }
}
