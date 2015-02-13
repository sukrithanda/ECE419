ECE419 Lab 2 README
Babneet Singh (998347061)
Sukrit Handa (998460810)

1. How to compile code?
	
	make clean; make

2. How to start the server?

	java MainServer $portnumber

	Example for $portnumber => 8000 

3. How to start the client side GUI interface?

	java Mazewar

4. Description of design components -

	a) MainServer class is responsible for server-side interface
	b) Mazewar is responsible for client-side interface

5. For communication between server-side and client-side, we
   created a class named DataPacket. DataPacket class is used
   to send consistent information about client actions to and 
   from the server. We created ConnectServer class for establishing
   connection, and for sending and receiving data.

6. On the client side, we used two queues for managing incoming
   and outgoing DataPackets. Outgoing DataPackets consisted of
   user actions while playing the game. We updated GUIClient class 
   to update the output queue instead of directly executing client  
   actions. These DataPackets were sent to the server. Incoming  
   DataPackets consisted of actions/operations that the GUI client  
   was suppose to execute. These DataPackets were received from the  
   server. In other words, server informs all clients on what to do 
   in order to keep them in sync. On the client side, we created two 
   threads: one to send data to the server (ClientSenderThread class), 
   and other to receive data from the server (ClientReceiverThread class). 
   On the server side, we only used one queue for managing incoming and  
   outgoing DataPackets. We created ServerBroadcastThread class to send  
   and receive the DataPackets. On the server side, we also developed 
   ClientTracker class to keep track of all clients and their starting
   points.

7. In order to appropriately interpret and respond to DataPackets,
   on the client we created ClientActionOperator class whereas on 
   the server side we created ServerThread class. ClientActionOperator
   class analyzed the DataPacket received from the server, and then
   executed game operations using functions defined in MazeImpl class. 
   ServerThread class analyzes the DataPackets received from each 
   client, and then sends global instructions to all clients simultaneously
   in order to maintain order.

8. There was a bug in the game which would cause unexpected errors when
   two players shoot at each other at the same time. Due to this bug, we 
   were not able to test our code for synchronized missile movements. Thus,
   we had to comment the code for synchronized missile movements. We removed
   the run() method in MazeImpl class, and switched it with a function named 
   missileTick(). We added another operation flag called MISSILE_TICK in our 
   DataPacket class. Whenever the server encounters a FIRE signal from a client, 
   it then initiates a thread which sends MISSILE_TICK messages to all clients
   after every 200 milliseconds. On the client side, ClientActionOperator class
   executes the missileTick() function when it receives a MISSILE_TICK action from 
   the server. When a missile hits a wall or kills a player, then the server side
   thread responsible for sending MISSILE_TICK message is terminated.
