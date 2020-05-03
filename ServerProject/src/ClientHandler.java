/*  ClientHandler.jave
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Observer;
import java.util.Observable;

class ClientHandler implements Runnable, Observer {

  private Server server;
  private Socket clientSocket;
  private BufferedReader fromClient;
  private PrintWriter toClient;
  private int clientID = -1;

  // parameterized constructor used to blackbox the networking between server and client
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
    System.out.println("Sending to client #" + clientID + ": " + string);
    toClient.println(string);
    toClient.flush();
  }

  @Override
  public void run() {
    String input;
    try {
      while ((input = fromClient.readLine()) != null) {
        System.out.println("From client: " + input);
        server.processRequest(input);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void update(Observable o, Object arg) {
    this.sendToClient((String) arg);
  }
}