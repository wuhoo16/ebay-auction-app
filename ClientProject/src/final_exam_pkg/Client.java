/*  Client.java
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */
package final_exam_pkg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Application {
	private static String host = "127.0.0.1"; // default local host
	private static Socket socket;
	private static BufferedReader fromServer;
	private static PrintWriter toServer;
//	private Scanner consoleInput = new Scanner(System.in);
	private static Queue<String> commandBuffer = new LinkedList<String>();
	private static boolean isFirstClickOfLoginField = true;
	private static boolean isFirstClickOfPasswordField = true;
	private static String username = null;
	static class Item {
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
			highestBidderUsername = "N/A";
			this.duration = duration;
			if (duration > 0) {
				isBiddable = true;
			}
		}
		// new parameterized constructor
		protected Item (String name, String description, double minPrice, double currentBidPrice, String highestBidderUsername, double duration) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.currentBidPrice = currentBidPrice;
			this.highestBidderUsername = new String(); this.highestBidderUsername += highestBidderUsername;
			this.duration = duration;
			if (duration > 0) {
				isBiddable = true;
			}
		}
		
	}
	private static ArrayList<Item> itemList = new ArrayList<Item> ();
	private static boolean isItemListInitialized = false;
	private static boolean isItemListChanged = false;
	private static ObservableList<Item> observableItemList;
	private static ArrayList<String> itemNamesList = new ArrayList<String> ();
	private static HashSet<String> watchlistItems = new HashSet<String>();
	private static ArrayList<Pair<String,Label>> watchlistInfoNodes = new ArrayList<Pair<String,Label>>();
	
	public Client() {
		host = "127.0.0.1"; // default local host // TODO: later change to init. dynamically based on text input from login menu
//		consoleInput = new Scanner(System.in); // TODO: input will not be from console, change to get from GUI buttons
	}
  
	@Override
	public void start(Stage primaryStage) {
//		// set up this client's socket connection with server
//		try {
//			Client.setUpSocketConnection();
//			sendToServer("initializeItemList"); // set initialize Item menu as first command
//		} catch (Exception e) {
//			System.out.println("exception when setting up socket connection");
//			e.printStackTrace();
//		}
		
		
		// LOGIN SCREEN SECTION BELOW
		// The nodes are organized on a borderpane, where the top is set to the welcomeBox and center is set to credentialsBox
		//========================================================================================================================
		BorderPane loginPane = new BorderPane();
		
		// Initialize nodes for top of loginPage borderPane
		Label greeting = new Label();
		Label signIn = new Label();
		greeting.setText("Welcome to Ebae!");
		greeting.setFont(new Font("Segoe UI Bold", 24));
//		greeting.setAlignment(Pos.BASELINE_LEFT); 
		signIn.setText("Sign in to continue:");
		signIn.setFont(new Font("Segoe UI", 16));
//		signIn.setAlignment(Pos.BASELINE_LEFT); 
		VBox welcomeBox = new VBox(2, greeting, signIn);
		welcomeBox.setAlignment(Pos.CENTER); 
		loginPane.setTop(welcomeBox);
		
		// Initialize nodes for center of loginPage borderPane
		TextField loginField = new TextField(); // login field node
		loginField.setText("Email or username");
		loginField.setStyle("-fx-text-fill: grey; font-style: italic");
		loginField.setFont(new Font("Segoe UI", 12));
		loginField.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) { // wipe the gray text once user clicks login textfield
				if (isFirstClickOfLoginField) {
					loginField.clear();
				}
				isFirstClickOfLoginField = false;
			}
		});
		TextField passwordField = new TextField(); // password field node
		passwordField.setText("Password");
		passwordField.setStyle("-fx-text-fill: grey; font-style: italic");
		passwordField.setFont(new Font("Segoe UI", 12));
		passwordField.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) { // wipe the gray text once user clicks login textfield
				if (isFirstClickOfPasswordField) {
					passwordField.clear();
				}
				isFirstClickOfPasswordField = false;
			}
		});
		Button signInButton = new Button("Sign in"); // sign-in button node
		Label signInErrorMsg = new Label();
		HBox signInBox = new HBox (10, signInButton, signInErrorMsg);
		
		signInErrorMsg.setStyle("-fx-text-fill: red");
		signInButton.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 16px");
		signInButton.setAlignment(Pos.CENTER); 
		signInButton.setOnAction(new EventHandler<ActionEvent>() { 
			@Override
			public void handle(ActionEvent event) {
				username = loginField.getText();
				String password = passwordField.getText();

				if (!username.equals("") && !username.equals("Email or username") && !password.equals("") && !password.equals("Password")) {
					signInErrorMsg.setText("");
					primaryStage.setTitle("Ebae Auction Site");
					primaryStage.setScene(generateAuctionScene()); 
					primaryStage.setMaximized(true);
					primaryStage.show(); 
				}
				else {
					signInErrorMsg.setText("Error! Please enter a valid username and password");
				}
			}
    	});
		Button guestButton = new Button("Continue as guest"); // guest button node
		guestButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				signInErrorMsg.setText("");
				username = "Guest";
				primaryStage.setTitle("Ebae Auction Site");
				primaryStage.setScene(generateAuctionScene());
				primaryStage.setMaximized(true);
				primaryStage.show(); 
			}
    	});
		Button quitButton = new Button("Quit"); // quit button node
		quitButton.setPrefSize(70,  30);
		quitButton.setStyle("-fx-text-fill: red; font-weight: bold; -fx-font-size: 16px");
		quitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Client.sendToServer("removeObserver");
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Platform.exit();
//				System.exit(0);
			}
    	});
		VBox credentialsBox = new VBox(5, loginField, passwordField, signInBox, guestButton, quitButton);
		credentialsBox.setMaxSize(375, 900);
		VBox.setMargin(loginField, new Insets(15, 0, 0, 10));
		VBox.setMargin(passwordField, new Insets(0, 0, 0, 10));
		VBox.setMargin(signInBox, new Insets(0, 0, 0, 10));
		VBox.setMargin(guestButton, new Insets(10, 0, 0, 10));
		VBox.setMargin(quitButton, new Insets(800, 0, 0, 10));
		loginPane.setCenter(credentialsBox);
		
		
		// Create initial login scene and place on the stage
		Scene loginScene = new Scene(loginPane, 450, 300);
		primaryStage.setTitle("Auction Login Page"); // Set the stage title 
		primaryStage.setScene(loginScene); // Place the scene in the stage
		primaryStage.setMaximized(true);
		primaryStage.show(); // Display the stage 
		
	}
	
	
	// AUCTION PAGE SCREEN BELOW
	// This method will generate all the nodes for the auction page and return the populated Scene
	// Call this right after the user successfully signs-in
	private static Scene generateAuctionScene() {
		// Declare nodes for AUCTION PAGE
		ScrollPane scrollWindow = new ScrollPane(); // watchlist window node
		ListView<VBox> itemView = new ListView<VBox>();
		itemView.setMinWidth(1890);
		itemView.setMaxSize(1890, 750);
		
		// CONTROLLER ROW 1 NODES
		ChoiceBox<String> itemMenu = new ChoiceBox<String>(); // item menu node
		itemMenu.setPrefHeight(35);
//		while (!isItemListInitialized) { 
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		for (Item item : itemList) {
			itemNamesList.add(item.name);
		}
		itemMenu.getItems().addAll(itemNamesList);
		Button addItemButton = new Button("Add this item to watchlist"); 
		addItemButton.setPrefHeight(35);
		Label addItemErrorMessage = new Label();
		addItemErrorMessage.setStyle("-fx-text-fill: red");
		
		
		// CONTROLLER ROW 2 NODES
		Label bidInstruction = new Label("Enter bid amount: $");
		bidInstruction.setStyle("-fx-text-fill: black; -fx-font-size: 18px");
		bidInstruction.setAlignment(Pos.CENTER);
		TextField bidField = new TextField();
		bidField.setPrefHeight(35);
		Pattern pattern = Pattern.compile("\\d*\\.?\\d{0,2}");
		TextFormatter formatter = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> {
		    return pattern.matcher(change.getControlNewText()).matches() ? change : null;
		});
		bidField.setTextFormatter(formatter);
		Button bidButton = new Button("Place bid");
		bidButton.setPrefHeight(35);
		Label bidErrorMessage = new Label();
		bidErrorMessage.setStyle("-fx-text-fill: red");
		
		
		// ALL BUTTON HANDLERS ARE BELOW:
		//===================================================================================
		addItemButton.setOnAction(new EventHandler<ActionEvent>() { // add-item button handler
			@Override
			public void handle(ActionEvent event) {
				String chosenItemName = itemMenu.getValue();
				if (!watchlistItems.contains(chosenItemName)) { // item has not been added to watchlist yet -> add to watchlist
					clearErrorMessages(addItemErrorMessage, bidErrorMessage);
					Item chosenItem = null;
					for (Item item: itemList) {
						if (item.name.contentEquals(chosenItemName)) chosenItem = item;
					}
					Label itemNameLabel = new Label(chosenItem.name);
					itemNameLabel.setStyle("-fx-text-fill: black; font-weight: bold; -fx-font-size: 16pt");
					Label itemDescription = new Label (chosenItem.description);
					String currentBidPrice = "N/A";
					if (chosenItem.currentBidPrice != 0.00) {
						currentBidPrice = new String(); currentBidPrice += "$" + String.valueOf(chosenItem.currentBidPrice);
					}
					Label itemInfo = new Label ("Minimum Bidding Price: $" + chosenItem.minPrice + "  Current Bid: " + currentBidPrice + "  Highest Bidder: " + chosenItem.highestBidderUsername + "  Time left: " + chosenItem.duration + " mins");
					Separator itemDivider = new Separator();
					VBox itemNode = new VBox(2, itemNameLabel, itemDescription, itemInfo, itemDivider);
					itemView.getItems().add(itemNode);
					watchlistItems.add(chosenItemName);
					watchlistInfoNodes.add(new Pair<String, Label>(chosenItemName, itemInfo));
//	    			observableItemList.addListener((ListChangeListener<? super Item>) itemNode);
				}
				else { // else, item is already in watchlist window
					addItemErrorMessage.setText("ERROR! " + chosenItemName + " is already added to the watchlist.");
				}
			}
			
		});
		
		bidButton.setOnAction(new EventHandler<ActionEvent> () { // palce-bid button handler
			@Override
			public void handle(ActionEvent event) {
					String chosenItemName = itemMenu.getValue();
					Double userBid = Double.parseDouble(bidField.getText());
					try {
						Item chosenItem = null;
						for (Item item: itemList) {
							if (item.name.contentEquals(chosenItemName)) chosenItem = item;
						}
						if (userBid <= chosenItem.minPrice) { // inputted bid is too low
							bidErrorMessage.setText("INVALID BID! Your bid must be higher than the minimum bidding price.");
						}
						else if (userBid <= chosenItem.currentBidPrice) {
							bidErrorMessage.setText("INVALID BID! Your bid must be higher than the current bid.");
						}
						else if (chosenItem.duration <= 0.00) {
							bidErrorMessage.setText("INVALID BID! Auction for this item has closed.");
						}
						else { // userBid is a valid bid
							clearErrorMessages(addItemErrorMessage, bidErrorMessage);
							isItemListChanged = false;
							sendToServer("updateBidPrice|" + chosenItemName + "|" + String.valueOf(userBid) + "|" + username + "|");
							bidErrorMessage.setText("BID SUCCESSFUL! You are now the highest bidder.");
							bidField.clear();
						}
					} catch (NullPointerException e) {
						System.out.println("Null pointer exception!");
					}
					
			}
		});
		
		
		// Final Layout Specification from top to bottom: welcomeMessage, controller, scrollWindow (containing itemView VBox)
		HBox controllerRow1 = new HBox(5, itemMenu, addItemButton, addItemErrorMessage);  // controller node
		controllerRow1.setAlignment(Pos.CENTER);
		HBox controllerRow2 = new HBox(5, bidInstruction, bidField, bidButton, bidErrorMessage);
		controllerRow2.setAlignment(Pos.CENTER);
		VBox controller = new VBox(5, controllerRow1, controllerRow2); // controller node (with all user interface)
		scrollWindow.setContent(itemView);
		scrollWindow.setPannable(true);
		Label welcomeMessage = new Label("Welcome to the auction, " + Client.username + "!");
		welcomeMessage.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 24pt");
		welcomeMessage.setTextAlignment(TextAlignment.CENTER);
		Separator divider = new Separator();
		VBox grid = new VBox(5, welcomeMessage, controller, divider, scrollWindow);
		VBox.setMargin(scrollWindow, new Insets(10));

		
		// Helper threads to handle button enabling and periodically update itemList
		Thread enableButtonThread = new Thread(new Runnable () {
			@Override
			public void run() {
				while (true) { // while the itemView has nodes added to it, keep queuing commands to update the client's itemList
					if (itemMenu.getValue() == null) {
						addItemButton.setDisable(true); 
						bidButton.setDisable(true);
					}
					else {
						addItemButton.setDisable(false);
						if (bidField.getText().isEmpty()) {
							bidButton.setDisable(true);
						}
						else bidButton.setDisable(false);
					}
				}
			}
		});
		Thread itemListUpdaterThread = new Thread(new Runnable () {
			@Override
			public void run() {
				while (!itemView.getItems().isEmpty()) { // while the itemView has nodes added to it, keep queuing commands to update the client's itemList
					sendToServer("updateItemList"); // update once per second
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
				}
			}
		});
		Thread updateWatchlistThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					for (Pair<String,Label> nodePair : watchlistInfoNodes) {
						String itemName = nodePair.getKey();
						System.out.println(itemName);
						Label infoNode = nodePair.getValue();
						System.out.println(infoNode.getText());
						for (Item item : itemList) {
							if (itemName.contentEquals(item.name)) {
								Platform.runLater(() -> {
									infoNode.setText("Minimum Bidding Price: $" + item.minPrice + "  Current Bid: $" + item.currentBidPrice + "  Highest Bidder: " + item.highestBidderUsername +  "  Time left: " + item.duration + " mins");
								});
							}
						}
					}
					try { // let other threads run for 0.5 ms, then update GUI
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
        });
		enableButtonThread.start();
		itemListUpdaterThread.start();
		updateWatchlistThread.start();
		
		
		// create and return auction Scene
		Rectangle2D screenBounds = Screen.getPrimary().getBounds();
		Scene auctionScene = new Scene(grid, screenBounds.getMaxX(), screenBounds.getMaxY());
		return auctionScene;
	}
	
	
	// helper method that clears all error messages in the auction screen GUI
	// usage: call this method after every successful user button press
	private static void clearErrorMessages(Label addItemErrorMessage, Label bidErrorMessage) {
		addItemErrorMessage.setText("");
		bidErrorMessage.setText("");
	}
	
  
	// set up connection to server through socket 4242
	private static void setUpSocketConnection() throws Exception {
	    socket = new Socket(host, 4242);
	    System.out.println("Connecting to server... " + socket);
	    toServer = new PrintWriter(socket.getOutputStream());
	    fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	
	    // readerThread handles all communication from the server
	    Thread readerThread = new Thread(new Runnable() {
	    	@Override
	    	public void run() {
	    		String input;
	    		try {
	    			while (!socket.isClosed() && (input = fromServer.readLine()) != null) {
	    				System.out.println("From server: " + input);
	    				processRequest(input);
	    			}
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
	    });
	    // writerThread handles writes all client commands to the server
	    Thread writerThread = new Thread(new Runnable() {
	    	@Override
	        public synchronized void run() {
	    		while (true) {
	        	    if (!commandBuffer.isEmpty()) {
		    			String topCommand = commandBuffer.remove();
//		            	GsonBuilder builder = new GsonBuilder();
//		            	Gson gson = builder.create();
		    			sendToServer(topCommand);
	        	    }
	    		}
	        }
	    });
	    readerThread.start();
	    writerThread.start();
	    
	}

	
	// format of messages returned from server will be: <command+"Successful">|<inputDataString>
	// command and each part of data is separated by |
    protected static void processRequest(String input) {
    	String[] inputArr = input.split("\\|"); // split input string around pipe character
    	
    	// TODO: use inputArr[0] to decide how to use the information from the server below:
    	switch (inputArr[0]) {
    		case "initializeItemListSuccessful":
    		case "updateItemListSuccessful":
    			itemList.clear();
 				String name = "";
				String description = "";
				Double minPrice = 0.00;
				Double currentBidPrice = 0.00;
				String highestBidderUsername = "";
				Double duration = 0.00;
    			for (int i = 1; i < inputArr.length; i++) {
    				if (i % 6 == 1) {
    					name += inputArr[i];
//    					itemNamesList.add(name);
    				}
    				else if (i % 6 == 2) {
    					description += inputArr[i];
    				}
    				else if (i % 6 == 3) {
    					minPrice = Double.parseDouble(inputArr[i]);
    				}
    				else if (i % 6 == 4) {
    					currentBidPrice = Double.parseDouble(inputArr[i]);
    				}
    				else if (i % 6 == 5) {
    					highestBidderUsername = inputArr[i];
    				}
    				else {
    					duration = Double.parseDouble(inputArr[i]);
    	 				itemList.add(new Item(name, description, minPrice, currentBidPrice, highestBidderUsername, duration));
    	 				name = "";
        				description = "";
        				highestBidderUsername = "";
    				}
    			}
    			isItemListChanged = true; // set changed flag
    			isItemListInitialized = true;
//    			observableItemList = FXCollections.observableList(itemList);
    			break;
    	}//end of switch
    	
    	return;
    }

  
	public static void main(String[] args) {
		// set up this client's socket connection with server
		try {
			Client.setUpSocketConnection();
			sendToServer("initializeItemList"); // set initialize Item menu as first command
		} catch (Exception e) {
			System.out.println("exception when setting up socket connection");
			e.printStackTrace();
		}
		
		// Launch Java FX Application thread
		launch(args);
	}
	
	
    protected static synchronized void sendToServer(String string) {
    	System.out.println("Sending to server: " + string);
    	toServer.println(string);
    	toServer.flush();
    }

}