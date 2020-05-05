/* (Server.java)
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */
package final_exam_pkg;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Scanner;

class Server extends Observable {
	
	private Integer numClients = 0;
	class Item {
		public String name;
		public String description;
		public double minPrice;
		public double currentBidPrice;
		public String highestBidderUsername;
		public double duration;
		public boolean isBiddable;
		
		protected Item (String name, String description, double minPrice, double duration) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.currentBidPrice = 0.00;
			this.highestBidderUsername = "N/A";
			this.duration = duration;
			if (duration > 0) {
				isBiddable = true;
			}
		}
	}
	protected ArrayList<Item> itemList = new ArrayList<Item>();
	protected ArrayList<ClientHandler> observerList = new ArrayList<ClientHandler>();
	
	public static void main(String[] args) {
		Server server = new Server();
		server.getItemsFromFile();
		server.startServer();
	}
	
	// stores data from the input file named "AuctionItemsInput" into an ArrayList
	// Input file format: name, description, minimum bidding price, and duration on separate lines for each item
	private void getItemsFromFile() {
		File inputFile = new File("src/AuctionItemsInput");
		try {
			Scanner fileScanner = new Scanner(inputFile);
			
			while (fileScanner.hasNextLine()) {
				String name = fileScanner.nextLine();
//				System.out.println(name);
				String description = fileScanner.nextLine();
//				System.out.println(description);
				double minPrice = Double.valueOf(fileScanner.nextLine());
//				System.out.println(minPrice);
				double duration = Double.valueOf(fileScanner.nextLine());
//				System.out.println(duration);
				itemList.add(new Item(name, description, minPrice, duration));
			}
		} catch (FileNotFoundException e) {
			System.out.println("ERROR! Specified input file does not exist!");
		} catch (NumberFormatException e) {
			System.out.println("ERROR! Input file is in the wrong format. Make sure all prices and durations are double types.");
		}
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
      observerList.add(handler);

      Thread t = new Thread(handler);
      t.start();
    }
  }
  
  // process inputs from the client
  // Input must be in the format: command stringData
  protected synchronized void processRequest(String input) {
	  String outputString = "";
	  if (!input.contains("|")) { // use parsing around spaces as delimiter (if it is not a special input that might contain whitespace)
		  String[] inputArr = input.trim().split(" ");
		  switch (inputArr[0]) {
		  	case "initializeItemList" :
		  		int clientIDToInit = Integer.parseInt(inputArr[1]);
		  		ClientHandler handlerToInit = null;
		  		for (ClientHandler observer : observerList) {
		  			if (observer.clientID == clientIDToInit) {
		  				handlerToInit = observer;
		  			}
		  		}
		  		outputString += "initializeItemListSuccessful|" + itemListToString();
		  		handlerToInit.sendToClient(outputString);
				break;
				
		  	case "updateItemList":
		  		int clientIDToUpdate = Integer.parseInt(inputArr[1]);
		  		ClientHandler handlerToUpdate = null;
		  		for (ClientHandler observer : observerList) {
		  			if (observer.clientID == clientIDToUpdate) {
		  				handlerToUpdate = observer;
		  			}
		  		}
		  		outputString += "updateItemListSuccessful|" + itemListToString();
		  		handlerToUpdate.sendToClient(outputString);
		  		break;
		  		
		  	case "removeObserver":
		  		int clientID = Integer.parseInt(inputArr[1]);
		  		ClientHandler handlerToRemove = null;
		  		for (ClientHandler observer : observerList) {
		  			if (observer.clientID == clientID) {
		  				handlerToRemove = observer;
		  			}
		  		}
		  		this.deleteObserver(handlerToRemove);
		  		observerList.remove(handlerToRemove);
		  		numClients--;
		  		break;
		  		
		  	default:
		  		
		  }
	  }
	  else { // else, input must be in the format:  command|itemName|newBidValue|higestBidderUsername|
		  String[] inputArr = input.trim().split("\\|");
//		  System.out.println("0th element: " + inputArr[0]);
//		  System.out.println("1th element: " + inputArr[1]);
//		  System.out.println("2th element: " + inputArr[2]);
//		  System.out.println("3th element: " + inputArr[3]);
		  switch (inputArr[0]) {
		  	case "updateBidPrice" :
		  		for (Item item : itemList) {
		  			if (item.name.contentEquals(inputArr[1])) {
		  				item.currentBidPrice = Double.parseDouble(inputArr[2]);
		  				item.highestBidderUsername = new String(); item.highestBidderUsername += inputArr[3]; 
		  			}
		  		}
		  		this.setChanged();
		  		this.notifyObservers("updateItemListSuccessful|" + itemListToString());
				break;
				
			default:
				
		  }
	  }
    	

	  
// 	  this.setChanged();
//	  this.notifyObservers(outputString);
  }
  
  // helper method that returns a string of all item names in itemList, each separated by a newline character
  private String itemListToString() {
	  String itemListString = "";
	  
	  for (Item item : itemList) {
		  itemListString += item.name + "|" + item.description + "|" + String.valueOf(item.minPrice) + "|" + String.valueOf(item.currentBidPrice) + "|" + item.highestBidderUsername + "|" + String.valueOf(item.duration) + "|";
	  }
	  return itemListString;
  }
  
}