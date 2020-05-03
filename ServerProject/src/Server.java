/* (Server.java)
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

class Server extends Observable {
	private Integer numClients = 0;

	public static void main(String[] args) {
		new Server().startServer();
	}

	private void startServer() {
	    try {
	    	setUpSocketConnections();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return;
	    }
	}

  /** setUpSocketConnections() method continuously checks for any clients that are trying to connect to the server through server socket 4242
   * Adds each clienthandler as an observer of the server. Each client handler gets a new thread.
   * 
   * @throws Exception
   * @return void
   */
  private void setUpSocketConnections() throws Exception {
    @SuppressWarnings("resource")
	ServerSocket serverSocket = new ServerSocket(4242);
    while (true) {
      Socket clientSocket = serverSocket.accept();
      System.out.println("Connecting to client... " + clientSocket);
      
      ClientHandler handler = new ClientHandler(this, clientSocket, numClients);
      numClients++;
      this.addObserver(handler);

      Thread t = new Thread(handler);
      t.start();
    }
  }
  
  // process inputs from the client
  // Input must be in the format: command stringInput number
  protected synchronized void processRequest(String input) {
    String output = "Error";
//    Gson gson = new Gson();
    String[] inputArr = input.split(" ");
    Message message = new Message(inputArr[0], inputArr[1], Integer.valueOf(inputArr[2]));
    try {
      String temp = "";
      switch (message.type) {
        case "upper":
          temp = message.input.toUpperCase();
          break;
        case "lower":
          temp = message.input.toLowerCase();
          break;
        case "strip":
          temp = message.input.replace(" ", "");
          break;
      }
      output = "";
      for (int i = 0; i < message.number; i++) {
        output += temp;
        output += " ";
      }
      this.setChanged();
      this.notifyObservers(output);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}