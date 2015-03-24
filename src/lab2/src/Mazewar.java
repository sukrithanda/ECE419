/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.io.Serializable;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private final int mazeSeed = 42;

        /**
         * The {@link Maze} that the game uses.
         */
        private Maze maze = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;
        
        /**
         * Important variables to connect to the server
         */
        public String name = null;
        public String hostname = null;
        public String port = null;
        	
        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        // thread to receive messages from the server
        public class ClientReceiverThread implements Runnable {

        	private ConnectServer connection = null;
        	final private GUIClient client;

        	/* This thread receives data packets from the server, and adds
        	 * them into the input queue */
            public void run() {
               for(;;) {
                    DataPacket data = (DataPacket) connection.receiveData();
                    client.input.add(data);
                    System.out.println("CLIENT_RECEIVER_THREAD: " + data.type + "; player id: " + data.id);
                }
            }
            
            public ClientReceiverThread (ConnectServer set_connection, GUIClient set_client) {
            	connection = set_connection;
            	client = set_client;
            }
        }
        
        // thread to send messages to the server
        public class ClientSenderThread implements Runnable {

        	private ConnectServer connection = null;
        	final private GUIClient client;
        	
        	/* This thread sends data packets in the output queue to the server */
            public void run() {
                for (;;) {
                    if (client.output.peek() != null) {
                    	DataPacket data = client.output.poll();
                        connection.sendData(data);
                        System.out.println("CLIENT_SENDER_THREAD: " + data.type + "; player id: " + data.id);
                    }
                }
            }
            
            public ClientSenderThread (ConnectServer set_connection, GUIClient set_client) {
            	connection = set_connection;
            	client = set_client;
            }
        }
        
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar() {
                super("ECE419 Mazewar");
                consolePrintLn("ECE419 Mazewar started!");
                
                // Create the maze
                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
                assert(maze != null);
                
                // Have the ScoreTableModel listen to the maze to find
                // out how to adjust scores.
                ScoreTableModel scoreModel = new ScoreTableModel();
                assert(scoreModel != null);
                maze.addMazeListener(scoreModel);
                
                /* Shows dialog boxes to get user and server data */
                getUserInputs();

                /* Convert port number from string into integer */
                int portNum = Integer.MIN_VALUE;
                try {
                	portNum = Integer.parseInt(port);
                } catch (NumberFormatException e) {
                	e.printStackTrace();
                }
                
                /* Check whether the port number is a valid input */
                if (portNum < 0) {
                	System.out.println("Error: negative port number");
                } else {
                	System.out.println("Port = " + portNum);
                }
                
                /* Establish connection with the server */
                final ConnectServer connection = new ConnectServer(portNum, hostname);

                // Create the GUIClient and connect it to the KeyListener queue
                guiClient = new GUIClient(name);
                // maze.addClient(guiClient);
                // this.addKeyListener(guiClient);

                /* This function starts the background threads to receive and send messages */
                initBackgroundThreads(connection);

                /* Setup the user environment and let the players start the game */
                setEnvToStartGame();

                // Create the panel that will display the maze.
                overheadPanel = new OverheadMazePanel(maze, guiClient);
                assert(overheadPanel != null);
                maze.addMazeListener(overheadPanel);
                
                // Don't allow editing the console from the GUI
                console.setEditable(false);
                console.setFocusable(false);
                console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
               
                // Allow the console to scroll by putting it in a scrollpane
                JScrollPane consoleScrollPane = new JScrollPane(console);
                assert(consoleScrollPane != null);
                consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
                
                // Create the score table
                scoreTable = new JTable(scoreModel);
                assert(scoreTable != null);
                scoreTable.setFocusable(false);
                scoreTable.setRowSelectionAllowed(false);

                // Allow the score table to scroll too.
                JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                assert(scoreScrollPane != null);
                scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
                
                // Create the layout manager
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                getContentPane().setLayout(layout);
                
                // Define the constraints on the components.
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1.0;
                c.weighty = 3.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                layout.setConstraints(overheadPanel, c);
                c.gridwidth = GridBagConstraints.RELATIVE;
                c.weightx = 2.0;
                c.weighty = 1.0;
                layout.setConstraints(consoleScrollPane, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1.0;
                layout.setConstraints(scoreScrollPane, c);
                                
                // Add the components
                getContentPane().add(overheadPanel);
                getContentPane().add(consoleScrollPane);
                getContentPane().add(scoreScrollPane);
                
                // Pack everything neatly.
                pack();

                // Let the magic begin.
                setVisible(true);
                overheadPanel.repaint();
                this.requestFocusInWindow();
        }

        
        private void setEnvToStartGame() {
			// TODO Auto-generated method stub
            guiClient.setupPlayer();

            new Thread () {
                public void run() {
                    String message = "";
                    while (!guiClient.init_check.get()
                                && !message.equals("start")) {
                        message = JOptionPane.showInputDialog("Enter \"start\"");
                    }
                    guiClient.playerReady();
                    System.out.println("Game starting ...");
                }
            }.start();
            
            while (!guiClient.start_check.get()) {
            }
		}

		private void initBackgroundThreads(ConnectServer connection) {
			// TODO Auto-generated method stub
			new Thread(new ClientSenderThread(connection, guiClient)).start();
            new Thread(new ClientReceiverThread(connection, guiClient)).start();
            new Thread(new ClientActionOperator(maze, this, guiClient)).start();
		}

		private void getUserInputs() {
			// TODO Auto-generated method stub
            // Throw up a dialog to get the GUIClient name.
            name = JOptionPane.showInputDialog("Enter your name");
            if((name == null) || (name.length() == 0)) {
              Mazewar.quit();
            }
            
            hostname = JOptionPane.showInputDialog("Enter hostname");
            if((hostname == null) || (hostname.length() == 0)) {
              Mazewar.quit();
            }

            port = JOptionPane.showInputDialog("Enter port number");
            if((port == null) || (port.length() == 0)) {
              Mazewar.quit();
            }
		}

		/**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) {

                /* Create the GUI */
                new Mazewar();
        }
}
