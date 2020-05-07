/*  ClientHandler.jave
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */
package final_exam_pkg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Observer;

import java.util.ArrayList;
import java.util.Observable;

import org.json.*;;

class ClientHandler extends Server implements Runnable, Observer {
	private Server server;
	protected Socket clientSocket;
	protected BufferedReader fromClient;
	protected PrintWriter toClient;
	protected int clientID = -1;

	// parameterized constructor used to blackbox the communication between server and clients
	protected ClientHandler(Server server, Socket clientSocket, int id) {
		this.server = server;
		this.clientSocket = clientSocket;
		this.clientID = id;
		try {
			fromClient = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			toClient = new PrintWriter(this.clientSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void sendToClient(String string) {
//    	System.out.println("Sending to client #" + clientID + ": " + string); // uncomment this to see commands sent to clients
    	toClient.println(string);
    	toClient.flush();
	}

	@Override
	public void run() {
		String input;
		try {
			while ((input = fromClient.readLine()) != null) {
//				System.out.println("From client: " + input); // uncomment this to see commands recieved from clients to console
				if (input.contentEquals("initializeItemList") || input.contentEquals("removeObserver") || input.contentEquals("updateItemList")) { input += " " + clientID;}
				server.processRequest(input);
			}
		} catch (IOException e) {
//			System.out.println("Client #" + clientID + "'s socket has closed and input stream no longer exists");
		} finally {
			System.out.println("Client #" + clientID + " has disconnected from the server.");
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		this.sendToClient((String) arg);
	}
}