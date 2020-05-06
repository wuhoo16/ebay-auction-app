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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Queue;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

class Server extends Observable {
	final static BigDecimal ONE_SECOND = new BigDecimal(1.0).divide(new BigDecimal(60.0), 100, RoundingMode.HALF_UP);
	private Integer numClients = 0;
	class Item {
		private String name;
		private String description;
		private double minPrice;
		private double currentBidPrice;
		private String highestBidderUsername;
		private BigDecimal duration;
		private String soldMessage;
//		private boolean isBiddable;
		
		private Item (String name, String description, double minPrice, BigDecimal duration) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.currentBidPrice = 0.00;
			this.highestBidderUsername = "N/A";
			this.duration = duration; // this will be the duration in minutes
			this.soldMessage = "";
//			if (duration > 0) {
//				isBiddable = true;
//			}
		}
	}
	protected ArrayList<Item> itemList = new ArrayList<Item>();
	protected ArrayList<Item> activeItemList = new ArrayList<Item>();
	Object activeItemListLock = new Object();
	protected Queue<Item> expiredItemQueue = new LinkedList<Item>();
	protected ArrayList<ClientHandler> observerList = new ArrayList<ClientHandler>();
	
	
	// Server's main method
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
				BigDecimal duration = new BigDecimal(Double.valueOf(fileScanner.nextLine()));
//				System.out.println(duration);
				itemList.add(new Item(name, description, minPrice, duration));
			}
			activeItemList.addAll(itemList);
		} catch (FileNotFoundException e) {
			System.out.println("ERROR! Specified input file does not exist!");
		} catch (NumberFormatException e) {
			System.out.println("ERROR! Input file is in the wrong format. Make sure all prices and durations are double types.");
		}
	}

	// initializes a timer that will decrement the duration value of ALL items every 1 second
	private void startServerTimer() {
		Server serverObject = this;
		Timer serverTimer = new Timer();
		serverTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				for (Item item : activeItemList) {
					if (item.duration.compareTo(ONE_SECOND) == 1) { // if duration remaining > 1 second
						item.duration = item.duration.subtract(ONE_SECOND); // equivalent to 1 second
				  		serverObject.setChanged();
				  		serverObject.notifyObservers("updateDurationSuccessful|" + item.name + "|" + item.duration);
					}
					else {
						item.duration = BigDecimal.ZERO;
						expiredItemQueue.add(item);
				  		serverObject.setChanged();
				  		serverObject.notifyObservers("updateDurationSuccessful|" + item.name + "|" + item.duration);
					}
				}
			}
		}, 0, 1000);
	}
	
	// initialize a separate timer that will which items' duration have hit 0 and remove them from the database/notify all clients that is is sold
	private void startExpirationTimer() {
		Server serverObject = this;
		Timer expirationUpdater = new Timer();
		expirationUpdater.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				for (Item item : activeItemList) {
					if (item.duration.compareTo(BigDecimal.ZERO) == 0) {
						System.out.println(item.name + " sold!");
						if (item.highestBidderUsername.contentEquals("N/A")) {
							item.soldMessage = "Item auction expired with no bidders!";
						}
						else {
							item.soldMessage = (item.name + " sold to " + item.highestBidderUsername + " for the final price of $" + item.currentBidPrice + "!");
						}
				  		serverObject.setChanged();
				  		serverObject.notifyObservers("notifyItemSoldSuccessful|" + item.name + "|" + item.soldMessage);
					}
				}
				while (!expiredItemQueue.isEmpty()) {
					activeItemList.remove(expiredItemQueue.remove()); // check if this is causing the concurrent modification exception
				}
			}
		}, 0, 500);
	}
	
	// called in main to start the server timer countdown and continuously network any incoming client connections
	private void startServer() {
	    try {
	    	startServerTimer();
	    	startExpirationTimer();
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
	  if (!input.contains("|")) { // use parsing around spaces as delimiter, input command will be in the format: <command> <clientID>
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
				
//		  	case "updateItemList":
//		  		int clientIDToUpdate = Integer.parseInt(inputArr[1]);
//		  		ClientHandler handlerToUpdate = null;
//		  		for (ClientHandler observer : observerList) {
//		  			if (observer.clientID == clientIDToUpdate) {
//		  				handlerToUpdate = observer;
//		  			}
//		  		}
//		  		outputString += "updateItemListSuccessful|" + itemListToString();
//		  		handlerToUpdate.sendToClient(outputString);
//		  		break;
		  		
		  	case "removeObserver":
		  		int clientID = Integer.parseInt(inputArr[1]);
		  		ClientHandler handlerToRemove = null;
		  		for (ClientHandler observer : observerList) {
		  			if (observer.clientID == clientID) {
		  				handlerToRemove = observer;
		  			}
		  		}
				try {
					handlerToRemove.toClient.flush();
					handlerToRemove.toClient.close();
					handlerToRemove.fromClient.close();
					handlerToRemove.clientSocket.close();
				} catch (IOException e) {
					System.out.println("Closing client's server-socket threw IOException.");
					e.printStackTrace();
				}
		  		this.deleteObserver(handlerToRemove);
		  		observerList.remove(handlerToRemove);
		  		numClients--;
		  		break;
		  		
		  	default:
		  		
		  }
	  }
	  else { // else, input must contain pipe characters and must be in the format:  command|itemName|newBidValue|higestBidderUsername
		  String[] inputArr = input.trim().split("\\|");
//		  System.out.println("0th element: " + inputArr[0]);
//		  System.out.println("1th element: " + inputArr[1]);
//		  System.out.println("2th element: " + inputArr[2]);
//		  System.out.println("3th element: " + inputArr[3]);
		  switch (inputArr[0]) {
		  	case "updateBidPrice":
		  		for (Item item : itemList) {
		  			if (item.name.contentEquals(inputArr[1])) {
		  				item.currentBidPrice = Double.parseDouble(inputArr[2]);
		  				item.highestBidderUsername = new String(); item.highestBidderUsername += inputArr[3]; 
				  		this.setChanged();
				  		this.notifyObservers("updateBidPriceSuccessful|" + item.name + "|" + item.currentBidPrice + "|" + item.highestBidderUsername);
				  		break;
		  			}
		  		}
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