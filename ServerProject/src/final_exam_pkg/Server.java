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
	// data fields:
	class Item {
		private String name;
		private String description;
		private double minPrice;
		private double currentBidPrice;
		private double buyNowPrice;
		private String highestBidderUsername;
		private BigDecimal duration;
		private String soldMessage;
		
		private Item (String name, String description, double minPrice, double buyNowPrice, BigDecimal duration) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.buyNowPrice = buyNowPrice;
			this.currentBidPrice = 0.00;
			this.highestBidderUsername = "N/A";
			this.duration = duration; // this will be the duration in minutes
			this.soldMessage = "Item is up for sale!";
		}
	}
	final static BigDecimal ONE_SECOND = new BigDecimal(1.0).divide(new BigDecimal(60.0), 100, RoundingMode.HALF_UP);
	private Integer numClients = 0;
	private ArrayList<Item> itemList = new ArrayList<Item>();
	private ArrayList<Item> activeItemList = new ArrayList<Item>();
	private Object activeItemListLock = new Object();
	private Queue<Item> expiredItemQueue = new LinkedList<Item>();
	private ArrayList<ClientHandler> observerList = new ArrayList<ClientHandler>();
	
	
	// methods:
	public static void main(String[] args) {
		Server server = new Server();
		server.startServer();
	}
	
	
	/**
	 * Set up all timers and then continuously handle networking for the server.
	 */
	private void startServer() {
	    try {
	    	getItemsFromFile();
	    	startServerTimer();
	    	startExpirationTimer();
	    	setUpSocketConnections();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return;
	    }
	}
	

	/**
	 * Reads item data from the input text file named "AuctionItemsInput" and stores into an ArrayList, itemList.
	 * NOTE: This method expects the input file to have the following format for each item: 
	 * Item name, item description, minimum bidding price, and item auction duration, all on SEPARATE LINES.
	 */
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
				double buyNowPrice = Double.valueOf(fileScanner.nextLine());
//				System.out.println(buyNowPrice);
				BigDecimal duration = new BigDecimal(Double.valueOf(fileScanner.nextLine()));
//				System.out.println(duration);
				itemList.add(new Item(name, description, minPrice, buyNowPrice, duration));
			}
			fileScanner.close();
			activeItemList.addAll(itemList);
		} catch (FileNotFoundException e) {
			System.out.println("ERROR! Specified input file does not exist!");
		} catch (NumberFormatException e) {
			System.out.println("ERROR! Input file is in the wrong format. Make sure all prices and durations are double types.");
		}
	}

	
	/**
	 * Starts a server timer that will decrement the duration value of all active items by 1 every second.
	 * Should be synchronized with the remove code in startExpirationTimer to prevent concurrent modification exceptions.
	 */
	private void startServerTimer() {
		Server serverObject = this;
		Timer serverTimer = new Timer();
		serverTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				synchronized(activeItemListLock) {
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
			}
		}, 0, 1000);
	}
	
	
	/**
	 * Starts another timer with timertask that will notify all clients when an item is sold, and remove sold/expired item from the server's activeItemList.
	 * NOTE: activeItemList.remove() should bes synchronized with the for-loop in startServerTimer to prevent concurrent modification.
	 */
	private void startExpirationTimer() {
		Server serverObject = this;
		Timer expirationUpdater = new Timer();
		expirationUpdater.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				synchronized (activeItemListLock) {
					for (Item item : activeItemList) {
						if (item.duration.compareTo(BigDecimal.ZERO) == 0) { // auction timer for this item has expired
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
						else if (item.currentBidPrice >= item.buyNowPrice) { // else, user has bid above the buy now threshold
							System.out.println(item.name + " sold immediately to " + item.highestBidderUsername);
							item.soldMessage = (item.name + " sold to " + item.highestBidderUsername + " for the final price of $" + item.currentBidPrice + "!");
					  		serverObject.setChanged();
					  		serverObject.notifyObservers("notifyItemSoldSuccessful|" + item.name + "|" + item.soldMessage);
					  		item.duration = BigDecimal.ZERO;
							expiredItemQueue.add(item);
					  		serverObject.setChanged();
					  		serverObject.notifyObservers("updateDurationSuccessful|" + item.name + "|" + item.duration);
						}
					}
					while (!expiredItemQueue.isEmpty()) {
						activeItemList.remove(expiredItemQueue.remove()); // check if this is causing the concurrent modification exception
					}
				}
			}
		}, 0, 50);
	}
	

	/**
	 * Continuously checks for any clients that are trying to connect to this server-socket bound to port 4242.
	 * Creates a clienthandler for each client and adds it to as an observer of the server. Each client handler runs on a new thread.
	 * @throws Exception
	 */
	private void setUpSocketConnections() throws Exception {
	    @SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(4242);
	    while (true) {
	    	// listen and accept any client connection requests
	    	Socket clientSocket = serverSocket.accept();
	    	System.out.println("Connecting to client... " + clientSocket);
	        
	    	// Create a ClientHandler for each client and starts a new thread for each clienthandler
	    	ClientHandler handler = new ClientHandler(this, clientSocket, numClients);
	    	numClients++;
	    	this.addObserver(handler);
	    	observerList.add(handler);
	    	Thread t = new Thread(handler);
	    	t.start();
	    }
	}
  

	/**
	 * Parses and processes input commands from the client. The first argument of the input must be one of the following:
     * "initializeItemList" , "removeObserver" , "updateBidPrice|"
	 * @param input representing the input command from the client
	 */
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
	  else { // else, input must contain pipe characters and must have data separated by pipe characters
		  String[] inputArr = input.trim().split("\\|");
		  switch (inputArr[0]) {
		  	case "updateBidPrice": // input in the format: updateBidPrice|itemName|newBidValue|higestBidderUsername
		  		for (Item item : activeItemList) {
		  			if (item.name.contentEquals(inputArr[1])) {
		  				item.currentBidPrice = Double.parseDouble(inputArr[2]);
		  				item.highestBidderUsername = new String(); item.highestBidderUsername += inputArr[3]; 
				  		this.setChanged();
				  		this.notifyObservers("updateBidPriceSuccessful|" + item.name + "|" + item.currentBidPrice + "|" + item.highestBidderUsername);
				  		break;
		  			}
		  		}
				break;
//		  	case "sellItemNow":// input in the format: sellItemNow|itemName|boughtPrice|higestBidderUsername
//		  		synchronized(activeItemListLock) {
//		  			for (Item item: activeItemList) {
//		  				if (item.name.contentEquals(inputArr[1])) {
//		  					item.duration = BigDecimal.ZERO;
//			  				item.currentBidPrice = Double.parseDouble(inputArr[2]);
//			  				item.highestBidderUsername = new String(); item.highestBidderUsername += inputArr[3]; 
//		  					break;
//		  				}
//		  			}
//		  		}
//				break;
			default:
		  }//end of switch
	   }
	}//end of processRequest
  
  
	/**
	 * Returns a string of all data in the itemList, with each new datatype separated by a pipe character.
     * NOTE: WHEN PARSING ON THE CLIENT SIDE, MAKE SURE TO SPLIT AROUND "\\|" since the pipe character is a special regular expression.
	 * @return String representation of all the contents of the server's itemList ArrayList.
	 */
	private String itemListToString() {
		String itemListString = "";
	    for (Item item : itemList) {
		    itemListString += item.name + "|" + item.description + "|" + String.valueOf(item.minPrice) + "|" + String.valueOf(item.currentBidPrice) + "|" + String.valueOf(item.buyNowPrice) + "|" + item.highestBidderUsername + "|" + String.valueOf(item.duration) + "|" + item.soldMessage + "|";
	    }
	    return itemListString;
	}
  
}//end of Server class