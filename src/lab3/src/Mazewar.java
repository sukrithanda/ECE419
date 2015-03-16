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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;

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
    private final int mazeWidth = 5; 

    /**
     * The default height of the {@link Maze}.
     */
    private final int mazeHeight = 5;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use 
     * the same seed value, or your mazes will be different.
     */
    private final int pointSeed = 35;
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
     * User Inputs
     */
    public String playerName;
    public String playerPortStr;
    public String nameServerHost;
    public String nameserverPortStr;
    
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

    int test = 1;
    
    public void print(String str) {
        if (test == 1) {
            System.out.println("DEBUG: (Mazewar) " + str);
        }
    }
    
    /**
     * Static method for performing cleanup before exiting the game.
     */
    public static void quit() {
        System.exit(0);
    }
   
    /** 
     * The place where all the pieces are put together. 
     */
    public Mazewar() {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Create the maze
        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed, pointSeed);
        assert(maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert(scoreModel != null);
        maze.addMazeListener(scoreModel);

        getUserInputs();

        int playerPort = Integer.parseInt(playerPortStr);
        int nameServerPort = Integer.parseInt(nameserverPortStr);

        // Connect to naming service
        print("creating MazewarP2PHandler");
        MazewarP2PHandler peerHandler = new MazewarP2PHandler(nameServerHost, nameServerPort,scoreModel, playerPort);
        maze.addClientHandler(peerHandler);

        print("creating lock");
        Lock lock = new ReentrantLock();
        maze.addLock(lock);

        // Create the GUIClient and connect it to the KeyListener queue
        print("creating gui client");
        guiClient = new GUIClient(playerName);
        print("adding gui client to chandler");
        guiClient.addClientHandler(peerHandler);

        // Register to lookup
        print("registering with maze");
        peerHandler.setMaze(maze);
        print("registering with lookup on port " + playerPort);
        peerHandler.playerEnrollNameServer(playerName, playerPort);

        peerHandler.localPlayer = guiClient;
        maze.addClient(guiClient);
        this.addKeyListener(guiClient);

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
        peerHandler.start();
        peerHandler.sendNewPlayerLocation();
    }

    public void getUserInputs() {
        playerName = JOptionPane.showInputDialog("Enter your name");
        if((playerName == null) || (playerName.length() == 0)) {
            Mazewar.quit();
        }

        playerPortStr = JOptionPane.showInputDialog("Enter player port");
        if((playerPortStr == null) || (playerPortStr.length() == 0)) {
            Mazewar.quit();
        }

        nameServerHost = JOptionPane.showInputDialog("Enter NameServer hostname");
        if((nameServerHost == null) || (nameServerHost.length() == 0)) {
            Mazewar.quit();
        }

        nameserverPortStr = JOptionPane.showInputDialog("Enter NameServer port");
        if((nameserverPortStr == null) || (nameserverPortStr.length() == 0)) {
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
