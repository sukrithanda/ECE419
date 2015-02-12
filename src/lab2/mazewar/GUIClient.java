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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

	    final public ConcurrentHashMap id_client = new ConcurrentHashMap();
	    public AtomicBoolean init_check = new AtomicBoolean(false);
	    public AtomicBoolean start_check = new AtomicBoolean(false);
	    final public Queue<DataPacket> input = new ConcurrentLinkedQueue<DataPacket>();
	    final public Queue<DataPacket> output = new ConcurrentLinkedQueue<DataPacket>();
	
		/**
         * Create a GUI controlled {@link LocalClient}.  
         */
        public GUIClient(String name) {
                super(name);
        }
        
        /**
         * Handle a key press.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyPressed(KeyEvent e) {
            DataPacket data = new DataPacket();
            data.name = getName();
            data.id = id;

            // If the user pressed Q, invoke the cleanup code and quit.
            if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
                System.exit(0);
            // Up-arrow moves forward.
            } else if(e.getKeyCode() == KeyEvent.VK_UP) {
            	data.type = data.FORWARD;
            // Down-arrow moves backward.
            } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            	data.type = data.BACKWARD;
            // Left-arrow turns left.
            } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            	data.type = data.LEFT;
            // Right-arrow turns right.
            } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            	data.type = data.RIGHT;
            // Spacebar fires.
            } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
            	data.type = data.FIRE;
            }

            output.offer(data);
        }
        
//        public void keyPressed(KeyEvent e) {
//                // If the user pressed Q, invoke the cleanup code and quit. 
//                if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
//                        Mazewar.quit();
//                // Up-arrow moves forward.
//                } else if(e.getKeyCode() == KeyEvent.VK_UP) {
//                        forward();
//                // Down-arrow moves backward.
//                } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
//                        backup();
//                // Left-arrow turns left.
//                } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
//                        turnLeft();
//                // Right-arrow turns right.
//                } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
//                        turnRight();
//                // Spacebar fires.
//                } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
//                        fire();
//                }
//        }
        
        /**
         * Handle a key release. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyReleased(KeyEvent e) {
        }
        
        /**
         * Handle a key being typed. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyTyped(KeyEvent e) {
        }

        public boolean setupPlayer () {
            output.offer(DataPacketConstructor(getName(), DataPacket.GET_ID));
            return true;
        }

        public boolean addPlayer () {
            output.offer(DataPacketConstructor(getName(), DataPacket.ADD_PLAYER, id));
            return true;
        }

        public boolean playerReady () {
            output.offer(DataPacketConstructor(getName(), DataPacket.PLAYER_READY, id));
            return true;
        }
        
        public boolean playerKilled (Direction direction, Point point) {
            output.offer(DataPacketConstructor(getName(), DataPacket.PLAYER_KILLED, id, direction, point));
            return true;
        }
        
        private static DataPacket DataPacketConstructor (String name, byte type) {
        	DataPacket data = new DataPacket();
            data.name = name;
            data.type = type;
            return data;
        }
        
        private static DataPacket DataPacketConstructor (String name, byte type, byte id) {
	        DataPacket data = new DataPacket();
	        data.type = type;
	        data.name = name;
	        data.id = id;
	        return data;
        }
        
        private static DataPacket DataPacketConstructor (String name, byte type, byte id, Direction direction, Point point) {
	        DataPacket data = new DataPacket();
	        data.type = type;
	        data.name = name;
	        data.id = id;
	        data.point = point;
	        data.direction = direction;
	        return data;
        }
}
