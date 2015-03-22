import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MazewarP2PHandler - Game controller (performs game operations) - Communicates
 * with NameServer - Handles incoming and outgoing messages
 */

public class MazewarP2PHandler extends Thread {

	/* Connect to naming service. */
	public MazewarP2PHandler(String nameserverHost, int nameserverPort,
			ScoreTableModel tableOfScores, int client_Port) {
		try {
			this.socket = new Socket(nameserverHost, nameserverPort);
			this.playersInfo = new ConcurrentHashMap<String, Client>();
			this.tableOfScores = tableOfScores;
			this.outgoingMessages = new ObjectOutputStream(
					socket.getOutputStream());
			this.incomingMessages = new ObjectInputStream(
					socket.getInputStream());
			ClientReciever clientReceiver = new ClientReciever(this,
					client_Port, manager, broadcaster);
			new Thread(clientReceiver).start();
		} catch (Exception e) {
			e.printStackTrace();
			print("Failed to connect to NameServer");
		}
	}

	Maze maze;
	ScoreTableModel tableOfScores;
	Socket socket;

	int playerID;
	Client localPlayer;

	ObjectInputStream incomingMessages;
	ObjectOutputStream outgoingMessages;

	DataPacketManager manager = new DataPacketManager();
	Broadcaster broadcaster = new Broadcaster(manager, this);

	ConcurrentHashMap<String, Client> playersInfo;
	ConcurrentHashMap<Integer, DataPacket> playerOps;

	DataPacket peerPacket;
	DataPacket nameServerPacket = new DataPacket();

	int orderVal;
	int LAMPORT_LIMIT = 25;
	DataPacket[] orderOps = new DataPacket[LAMPORT_LIMIT];

	boolean exitGame = false;
	int test = 1;

	void print(String str) {
		if (test == 1) {
			System.out.println("DEBUG: (MazewarP2PHandler) " + str);
		}
	}

	private boolean displayPlayerOperation(Client client) {
		boolean status = true;

		if (client != null) {
			client.acquireLock();
		}

		int type = peerPacket.scenarioType;

		if (DataPacket.PLAYER_LEFT == type) {

			movePlayerLeft(client);

		} else if (DataPacket.PLAYER_RIGHT == type) {

			movePlayerRight(client);

		} else if (DataPacket.PLAYER_BACK == type) {

			movePlayerBackward(client);

		} else if (DataPacket.PLAYER_FORWARD == type) {

			movePlayerForward(client);

		} else if (DataPacket.PLAYER_SPAWN == type) {

			playerSpawn();

		} else if (DataPacket.PLAYER_RESPAWN == type) {

			playerRespawn();

		} else if (DataPacket.PLAYER_FIRE == type) {

			playerFire(client);

		} else if (DataPacket.PLAYER_REGISTER == type) {

			recordPlayerOperation();

		} else if (DataPacket.PLAYER_QUIT == type) {

			playerExitOperation();

		} else {
			status = false;
			print("Unknown packet type" + peerPacket.scenarioType);
		}

		if (client != null) {
			client.freeLock();
		}

		print("Lock released");
		return status;
	}

	public void manageKeyboardCmd(KeyEvent event) {
		if ((event.getKeyChar() == 'Q') || (event.getKeyChar() == 'q')) {
			exitGame = true;
			try {
				DataPacket packetToClients = new DataPacket();
				DataPacket packetToLookup = new DataPacket();

				packetToClients.playerID = playerID;
				packetToClients.scenarioType = DataPacket.PLAYER_QUIT;
				broadcaster.peerSendMulticast(packetToClients);
				packetToLookup.playerID = playerID;
				packetToLookup.scenarioType = DataPacket.NS_QUIT;
				outgoingMessages.writeObject(packetToLookup);

				outgoingMessages.close();
				incomingMessages.close();
				socket.close();
				print("Player leaving game");
			} catch (Exception exception) {
				exception.printStackTrace();
				print("Player cannot leave game");
			}
			Mazewar.quit();
		} else if (event.getKeyCode() == KeyEvent.VK_DOWN
				&& !localPlayer.getKilled()) {
			dispatchPlayerOpToPeers(DataPacket.PLAYER_BACK);
		} else if (event.getKeyCode() == KeyEvent.VK_UP
				&& !localPlayer.getKilled()) {
			dispatchPlayerOpToPeers(DataPacket.PLAYER_FORWARD);
		} else if (event.getKeyCode() == KeyEvent.VK_RIGHT
				&& !localPlayer.getKilled()) {
			dispatchPlayerOpToPeers(DataPacket.PLAYER_RIGHT);
		} else if (event.getKeyCode() == KeyEvent.VK_LEFT
				&& !localPlayer.getKilled()) {
			dispatchPlayerOpToPeers(DataPacket.PLAYER_LEFT);
		} else if (event.getKeyCode() == KeyEvent.VK_SPACE
				&& !localPlayer.getKilled()) {
			dispatchPlayerOpToPeers(DataPacket.PLAYER_FIRE);
		}
	}

	private void movePlayerForward(Client client) {
		if (!client.getKilled()) {
			client.forward();
		} else {
			print("Forward Op - No player found with ID " + peerPacket.playerID);
		}
	}

	private void movePlayerBackward(Client client) {
		if (!client.getKilled()) {
			client.backup();
		} else {
			print("Backward Op - No player found with ID "
					+ peerPacket.playerID);
		}
	}

	private void movePlayerLeft(Client client) {
		if (!client.getKilled()) {
			client.turnLeft();
		} else {
			print("Left Op - No player found with ID " + peerPacket.playerID);
		}
	}

	private void movePlayerRight(Client client) {
		if (!client.getKilled()) {
			client.turnRight();
		} else {
			print("Right Op - No player found with ID " + peerPacket.playerID);
		}
	}

	private void playerFire(Client client) {
		if (!client.getKilled()) {
			client.fire();
		} else {
			print("Fire Op - No player found with ID " + peerPacket.playerID);
		}
	}

	public void playerEnrollNameServer(String playerName, int clientPort) {
		DataPacket dpOut = new DataPacket();

		try {
			dpOut.scenarioType = DataPacket.NS_REGISTER;
			dpOut.playerType = DataPacket.PLAYER_REMOTE;
			dpOut.playerName = playerName;
			dpOut.hostName = InetAddress.getLocalHost().getHostName();
			dpOut.portNum = clientPort;

			outgoingMessages.writeObject(dpOut);
			nameServerPacket = (DataPacket) incomingMessages.readObject();
			checkNameServerConnection();
		} catch (Exception e) {
			e.printStackTrace();
			print("Unable to connect to NameServer");
		}

	}

	public void robolEnroll(Client client) {
		DataPacket dpOut = new DataPacket();
		try {
			dpOut.scenarioType = DataPacket.PLAYER_REGISTER;
			dpOut.playerName = localPlayer.getName();
			dpOut.playerLocation = maze.getClientPoint(client);
			dpOut.playerDirection = localPlayer.getOrientation();
			dpOut.playerType = DataPacket.PLAYER_REMOTE;

			print("Successfully registered " + localPlayer.getName());
			outgoingMessages.writeObject(dpOut);
			playersInfo.put(localPlayer.getName(), localPlayer);
		} catch (IOException e) {
			e.printStackTrace();
			print("Unable to register");
		}

	}

	public void playerJoin() {
		DataPacket dpOut = new DataPacket();
		dpOut.scenarioType = DataPacket.PLAYER_REGISTER;
		dpOut.playerID = playerID;
		dpOut.hostName = playerOps.get(playerID).hostName;
		dpOut.portNum = playerOps.get(playerID).portNum;
		broadcaster.peerSendMulticast(dpOut);
	}

	public void sendNewPlayerLocation() {
		DataPacket dpOut = new DataPacket();
		DataPacket dpIn = new DataPacket();
		dpIn = localPlayerInfo();

		dpOut.scenarioType = DataPacket.PLAYER_SPAWN;
		dpOut.newPlayer = false;
		dpOut.playerID = playerID;
		dpOut.NSTable = new ConcurrentHashMap();
		dpOut.NSTable.put(playerID, localPlayerInfo());

		dpIn.player = localPlayer;
		dpIn.player.setId(playerID);
		playerOps.put(playerID, dpIn);
		broadcaster.peerSendMulticast(dpOut);
	}

	private void checkNameServerConnection() {
		if (DataPacket.ERR_RESERVED_NS_PORT == nameServerPacket.errType) {
			print("Port being used");
			Mazewar.quit();
		}

		playerID = nameServerPacket.playerID;
		playerOps = new ConcurrentHashMap<Integer, DataPacket>();
		playerOps = nameServerPacket.NSTable;

		if (!playerOps.isEmpty()) {
			Object[] hashKeys = playerOps.keySet().toArray();
			int size = playerOps.size();

			for (int i = 0; i < size; i++) {
				int hashKey = Integer.parseInt(hashKeys[i].toString());
				if (hashKey == playerID) {
					continue;
				}

				DataPacket client_data = playerOps.get(hashKey);
				String client_host = client_data.hostName;
				int client_port = client_data.portNum;
				print("Connecting player " + hashKey);

				Socket socket = null;
				try {
					socket = new Socket(client_host, client_port);
					manager.addOutputStream(
							new ObjectOutputStream(socket.getOutputStream()),
							hashKey);
					print("Connected to player " + playerID);
				} catch (Exception e) {
					print("Unable to connect to player " + playerID);
				}
			}
			playerJoin();
		}
	}

	public void playerSpawn(int playerScore, Integer playerID,
			ConcurrentHashMap<Integer, DataPacket> dataEntry) {
		DataPacket dp = new DataPacket();
		dp = dataEntry.get(playerID);

		RemoteClient remoteClient = new RemoteClient(dp.playerName);
		maze.addClient(remoteClient, dp.playerLocation, dp.playerDirection);
		tableOfScores.updateScore(remoteClient, playerScore);
		dp.player = remoteClient;
		dp.player.setId(playerID);
		playerOps.put(playerID, dp);
	}

	public void playerSpawn() {
		ConcurrentHashMap<Integer, DataPacket> dataEntry = peerPacket.NSTable;
		Integer playerID = peerPacket.playerID;
		DataPacket dp = new DataPacket();
		dp = dataEntry.get(playerID);

		RemoteClient remoteClient = new RemoteClient(dp.playerName);
		maze.addClient(remoteClient, dp.playerLocation, dp.playerDirection);
		dp.player = remoteClient;
		dp.player.setId(playerID);
		playerOps.put(playerID, dp);
	}

	private void playerRespawn() {
		Integer firePlayerID = peerPacket.playerFire;
		Integer victimPlayerID = peerPacket.playerDead;
		print("Player killer " + firePlayerID + "; Player victim "
				+ victimPlayerID);

		if (playerOps.containsKey(victimPlayerID)) {
			Client killerClient = (playerOps.get(firePlayerID)).player;
			Client victimClient = (playerOps.get(victimPlayerID)).player;

			killerClient.acquireLock();

			Point point = peerPacket.playerLocation;
			Direction direction = peerPacket.playerDirection;
			maze.configureClient(killerClient, victimClient, point, direction);
			victimClient.setKilled(false);

			killerClient.freeLock();
		} else {
			print("No player found with id: " + peerPacket.playerID);
		}
	}

	public void playerRespawn(DataPacket dpIn) {
		Integer victimPlayerID = dpIn.playerDead;
		Integer firePlayerID = dpIn.playerFire;
		print("Player killer " + firePlayerID + "; Player victim "
				+ victimPlayerID);

		if (playerOps.containsKey(victimPlayerID)) {
			Client killerClient = (playerOps.get(firePlayerID)).player;
			Client victimClient = (playerOps.get(victimPlayerID)).player;

			killerClient.acquireLock();

			Point point = dpIn.playerLocation;
			Direction direction = dpIn.playerDirection;
			maze.configureClient(killerClient, victimClient, point, direction);
			victimClient.setKilled(false);

			killerClient.freeLock();
		} else {
			print("No player found with id: " + dpIn.playerID);
		}
	}

	public void executePlayerOperation(Integer lampClk) {
		Integer lampClkCurrent = manager.getOpOrder();
		boolean runStatus;
		print("Recent player operation " + lampClkCurrent + ", Lamport Clock "
				+ lampClk);

		if (manager.getOpOrder() == lampClk) {
			int index = lampClk;
			while (orderOps[index] != null) {
				print("Executing player operation " + index);

				Client client = null;
				peerPacket = orderOps[lampClk];
				orderOps[lampClk] = null;
				if (peerPacket.scenarioType != DataPacket.PLAYER_SPAWN) {
					if (peerPacket.scenarioType != DataPacket.PLAYER_QUIT) {
						client = (playerOps.get(peerPacket.playerID)).player;
					}
				}

				runStatus = displayPlayerOperation(client);
				if (!runStatus) {
					break;
				}

				index = index + 1;
				if (LAMPORT_LIMIT == index) {
					index = 0;
				}
			}

			manager.setOpOrder(index);
			print("Player operation  " + index);
		}
	}

	private void recordPlayerOperation() {
		ConcurrentHashMap<String, DataPacket> playerList = nameServerPacket.playerList;
		String playerName = nameServerPacket.playerName;

		if (playerName.equals(localPlayer.getName())) {
			print("Player " + playerName + " already exists");
		} else {
			int playerType = nameServerPacket.playerType;
			print("Adding new player " + playerName);

			switch (playerType) {
			case DataPacket.PLAYER_REMOTE:
				RemoteClient remoteClient = new RemoteClient(playerName);
				playersInfo.put(playerName, remoteClient);
				maze.addClient(remoteClient, nameServerPacket.playerLocation,
						nameServerPacket.playerDirection);
				break;
			case DataPacket.PLAYER_ROBOT:
				break;
			default:
				print("No player Ops available");
				break;
			}
		}

		orderVal = nameServerPacket.orderVal;

		for (Map.Entry<String, DataPacket> mapElement : playerList.entrySet()) {
			String mapKey = mapElement.getKey();
			print(mapKey);
			if (!playersInfo.containsKey(mapKey)) {
				DataPacket dp = mapElement.getValue();
				switch (dp.playerType) {
				case DataPacket.PLAYER_REMOTE:
					RemoteClient remoteClient = new RemoteClient(mapKey);
					playersInfo.put(mapKey, remoteClient);
					maze.addClient(remoteClient, dp.playerLocation,
							dp.playerDirection);
					break;
				case DataPacket.PLAYER_ROBOT:
					break;
				default:
					break;
				}
			}
		}
	}

	private void dispatchPlayerOpToPeers(int type) {
		DataPacket dpOut = new DataPacket();
		dpOut.scenarioType = type;
		dpOut.playerID = playerID;
		dpOut.playerName = localPlayer.getName();
		broadcaster.peerSendMulticast(dpOut);
	}

	public boolean isLocalPlayer(Client client) {
		if (client == localPlayer) {
			return true;
		} else {
			return false;
		}
	}

	public void dispatchRespawnMessage(Point point, Direction direction,
			Integer killersID, Integer victimsID) {
		print("Respawning, Killer " + killersID + ", Victim " + victimsID);
		DataPacket dpOut = new DataPacket();
		dpOut.playerID = playerID;
		dpOut.scenarioType = DataPacket.PLAYER_RESPAWN;
		dpOut.playerLocation = point;
		dpOut.playerDirection = direction;
		dpOut.playerFire = killersID;
		dpOut.playerDead = victimsID;
		broadcaster.peerSendMulticast(dpOut);
	}

	public DataPacket localPlayerInfo() {
		DataPacket dp = new DataPacket();
		dp.playerID = playerID;
		dp.playerName = localPlayer.getName();
		dp.playerDirection = localPlayer.getOrientation();
		dp.playerLocation = maze.getClientPoint(localPlayer);
		return dp;
	}

	public void setMaze(Maze maze) {
		this.maze = maze;
	}

	public Integer localPlayerID() {
		return playerID;
	}

	public int localPlayerScore() {
		return tableOfScores.returnScore(playerOps.get(playerID).player);
	}

	public void recordPlayerOp(DataPacket dp) {
		orderOps[dp.lampClk] = dp;
		print("Recording player in ordered list " + dp.lampClk);
	}

	public void playerExitOperation() {
		print("Player exiting");
		Client client = (playerOps.get(peerPacket.playerID)).player;
		maze.removeClient(client);
	}
}
