/*  Client.java
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */
package final_exam_pkg;

import java.util.ArrayList;
import java.util.HashSet;
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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.DecimalFormat;

public class Client extends Application {
	private static final double MAX_WIDTH = Screen.getPrimary().getBounds().getMaxX();
	private static final double MAX_HEIGHT = Screen.getPrimary().getBounds().getMaxY();
	private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("#.##");
	private static final BigDecimal TEN_SECONDS = new BigDecimal(0.175);
	private static String host;
	private static Socket socket;
	private static BufferedReader fromServer;
	private static PrintWriter toServer;
	
//	private Scanner consoleInput = new Scanner(System.in);
//	private static Queue<String> commandBuffer = new LinkedList<String>();
//	private static boolean isFirstClickOfLoginField = true;
//	private static boolean isFirstClickOfPasswordField = true;
	private static boolean isIPFieldCleared = false;
	private static String username = null;
	static class Item {
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
			highestBidderUsername = "N/A";
			this.duration = duration;
			soldMessage = "Item is up for sale!";
//			if (duration > 0) {
//				isBiddable = true;
//			}
		}
		// new parameterized constructor
		private Item (String name, String description, double minPrice, double currentBidPrice, String highestBidderUsername, BigDecimal duration, String soldMsg) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.currentBidPrice = currentBidPrice;
			this.highestBidderUsername = new String(); this.highestBidderUsername += highestBidderUsername;
			this.duration = duration;
			this.soldMessage = soldMsg;
//			if (duration > 0) {
//				isBiddable = true;
//			}
		}
		
	}
	private static ArrayList<Item> itemList = new ArrayList<Item> ();
//	private static boolean isItemListInitialized = false;
//	private static ObservableList<Item> observableItemList;
	private static ArrayList<String> itemNamesList = new ArrayList<String> ();
	private static HashSet<String> watchlistItemNames = new HashSet<String>();
	private static ArrayList<Item> watchlistItems = new ArrayList<Item>();
	private static ArrayList<Pair<String, VBox>> watchlistInfoNodes = new ArrayList<Pair<String, VBox>>();
	private static boolean isItemListUpdated = false;
	private static boolean sessionDone = false;
	private static ArrayList<Thread> activeThreadList = new ArrayList<Thread>();
	
	// resets all of the Client class's data members before returning to the login scene
	private static void resetAllVariables() {
		isIPFieldCleared = false;
		username = null;
		itemList = new ArrayList<Item> ();
		itemNamesList = new ArrayList<String> ();
		watchlistItemNames = new HashSet<String>();
		watchlistItems = new ArrayList<Item>();
		watchlistInfoNodes  = new ArrayList<Pair<String, VBox>>();
		isItemListUpdated = false;
		sessionDone = false;
		activeThreadList  = new ArrayList<Thread>();
	}
	
	
	// default constructor (may not be called)
	public Client() {
		host = "127.0.0.1"; // default local host // TODO: later change to init. dynamically based on text input from login menu
//		consoleInput = new Scanner(System.in); // TODO: input will not be from console, change to get from GUI buttons
	}
	
  
	@Override
	public void start(Stage primaryStage) {
		// Generate initial login scene and place on the primary stage
		primaryStage.setTitle("Auction Login Page"); // Set the stage title 
		primaryStage.setScene(generateNewLoginScene(primaryStage)); // Place the scene in the stage
//		primaryStage.setMaximized(true);
		primaryStage.show(); // Display the stage 
	}
	
	
	// helper function that returns regular expression needed to create the ip field text formatter
    private static String getRegEx() {
        String firstBlock = "(([01]?[0-9]{0,2})|(2[0-4][0-9])|(25[0-5]))" ;
        String nextBlock = "(\\." + firstBlock+")" ;
        String ipAddress = firstBlock+ "?" + nextBlock + "{0,3}";
        return "^" + ipAddress;
    }
	
    
	// LOGIN PAGE SCENE BELOW
	// This method will initialize and return a new login scene
	private static Scene generateNewLoginScene(Stage primaryStage) {
		BorderPane loginPane = new BorderPane();
			
		// borderPane RIGHT nodes
		Button quitButton = new Button("Quit"); // quit button node
		quitButton.setPrefSize(70,  30);
		quitButton.setStyle("-fx-text-fill: red; font-weight: bold; -fx-font-size: 16px");
		loginPane.setRight(quitButton);
		BorderPane.setMargin(quitButton,  new Insets(10, 10, 0, 0));
		
		
		// borderPane CENTER-TOP nodes
		Label greeting = new Label();
		Label signIn = new Label();
		greeting.setText("Welcome to Ebae!");
		greeting.setFont(new Font("Segoe UI Bold", 24));
		signIn.setText("Sign in to continue:");
		signIn.setFont(new Font("Segoe UI", 16));
		VBox welcomeBox = new VBox(2, greeting, signIn);
		welcomeBox.setAlignment(Pos.CENTER); 
		VBox.setMargin(greeting,  new Insets(10, 0, 0, 0));
		
		
		// borderPane CENTER nodes
		TextField hostIPField = new TextField(); // IP field node
		hostIPField.setText("127.0.0.1"); // set the server ip address to be local machine by default (wipes if user clicks on the field)
		hostIPField.setStyle("-fx-text-fill: grey; font-style: italic");
		hostIPField.setFont(new Font("Segoe UI", 12));
		String regex = getRegEx();
		final UnaryOperator<TextFormatter.Change> ipFilter = e -> {
			return (e.getControlNewText().matches(regex) ? e : null);
		};
		hostIPField.setOnMousePressed(e -> { // wipes the ip field if user wants to connect to a remote server
				if (!isIPFieldCleared) {
					hostIPField.clear();
					hostIPField.setTextFormatter(new TextFormatter<>(ipFilter));
					isIPFieldCleared = true;
				}
		});
		TextField loginField = new TextField(); // login field node
		loginField.setPromptText("Email or username");
		loginField.setStyle("-fx-text-fill: grey; font-style: italic");
		loginField.setFont(new Font("Segoe UI", 12));
		TextField passwordField = new TextField(); // password field node
		passwordField.setPromptText("Password");
		passwordField.setStyle("-fx-text-fill: grey; font-style: italic");
		passwordField.setFont(new Font("Segoe UI", 12));
		Button signInButton = new Button("Sign in"); // sign-in button node
		Label signInErrorMsg = new Label();
		HBox signInBox = new HBox (10, signInButton, signInErrorMsg);
		
		signInErrorMsg.setStyle("-fx-text-fill: red");
		signInButton.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 16px");
		signInButton.setAlignment(Pos.CENTER); 
		signInButton.setOnAction(new EventHandler<ActionEvent>() { 
			@Override
			public synchronized void handle(ActionEvent event) {
				String inputUsername = loginField.getText();
				String password = passwordField.getText();
				String hostIP = hostIPField.getText();

				if (inputUsername.equals("")) {
					signInErrorMsg.setText("ERROR! Please enter a username");
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else if (password.equals("")) {
					signInErrorMsg.setText("ERROR! Please enter a password");
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else if (hostIP.contentEquals("")) {
					signInErrorMsg.setText("ERROR! Please enter a valid host IP address");
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else { // none of the blanks are empty		
					try {
						signInErrorMsg.setText("");
			    		setUpSocketConnection(hostIP);
			    		hostIP = "";
			    		sendToServer("initializeItemList"); // initialize Item menu as first command
			    		while (!isItemListUpdated) {System.out.println("Loading items from server database..."); Thread.yield();}
			    		username = inputUsername;
			    		primaryStage.setTitle("Auction Site"); 
						primaryStage.setScene(generateNewAuctionScene(primaryStage)); 
						primaryStage.show();
					} catch (Exception e) {
						System.out.println("ERROR! Exception thrown since Server IP address is invalid.");
						signInErrorMsg.setText("ERROR! Server refused to connect.");
						loginField.setText("");
						passwordField.setText("");
						hostIPField.setText("");
					}
				}
			}
    	});
		Button guestButton = new Button("Continue as guest"); // guest button node
		guestButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public synchronized void handle(ActionEvent event) {
				String inputUsername = loginField.getText();
				String password = passwordField.getText();
				String hostIP = hostIPField.getText();

				if (hostIP.contentEquals("")) {
					signInErrorMsg.setText("ERROR! Please enter a valid host IP address");
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else { // ip-field is not empty
					try {
						signInErrorMsg.setText("");
			    		setUpSocketConnection(hostIP);
			    		hostIP = "";
			    		sendToServer("initializeItemList"); // initialize Item menu as first command
			    		while (!isItemListUpdated) {System.out.println("Loading items from server database..."); Thread.yield();}
			    		username = "Guest";
			    		primaryStage.setTitle("Auction Site"); 
						primaryStage.setScene(generateNewAuctionScene(primaryStage)); 
						primaryStage.show();
					} catch (Exception e) {
						System.out.println("ERROR! Exception thrown since Server IP address is invalid.");
						signInErrorMsg.setText("ERROR! Server refused to connect.");
						loginField.setText("");
						passwordField.setText("");
						hostIPField.setText("");
					}
				}
			}
    	});
		quitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Platform.exit();
				System.exit(0);
			}
    	});
		
		
		// BorderPane Center Node's Alignment: Login field, password field, sign in button, and guest button
		VBox centerBox = new VBox(5, welcomeBox, hostIPField, loginField, passwordField, signInBox, guestButton);
		centerBox.setMaxSize(375, 900);
		VBox.setMargin(welcomeBox, new Insets(0, 0, 15, 10));
		VBox.setMargin(hostIPField, new Insets(0, 0, 0, 10));
		VBox.setMargin(loginField, new Insets(0, 0, 0, 10));
		VBox.setMargin(passwordField, new Insets(0, 0, 0, 10));
		VBox.setMargin(signInBox, new Insets(0, 0, 0, 10));
		VBox.setMargin(guestButton, new Insets(10, 0, 0, 10));
		loginPane.setCenter(centerBox);
		
		Scene loginScene = new Scene(loginPane, MAX_WIDTH, MAX_HEIGHT);
		return loginScene;
	}
	
	
	
	
	
	// AUCTION PAGE SCREEN BELOW
	// This method will generate all the nodes for the auction page and return the populated Scene
	// Call this right after the user successfully signs-in
	private static Scene generateNewAuctionScene(Stage primaryStage) {
		// Welcome Message Row 0 Nodes
		Label welcomeMessage = new Label("Welcome to the auction, " + Client.username + "!");
		welcomeMessage.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 24pt");
		Button logOutButton = new Button("Log out");
		logOutButton.setStyle("-fx-text-fill: red; font-weight: bold; -fx-font-size: 16px");
		logOutButton.setAlignment(Pos.CENTER_RIGHT);
		HBox welcomeMessageRow = new HBox(640, welcomeMessage, logOutButton);
		welcomeMessageRow.setAlignment(Pos.CENTER_RIGHT);
		HBox.setMargin(logOutButton, new Insets(10, 10, 0, 0));
		HBox.setMargin(welcomeMessage, new Insets(10, 0, 0, 0));
		
		// CONTROLLER ROW 1 NODES
		ChoiceBox<String> itemMenu = new ChoiceBox<String>(); // item menu node
		itemMenu.setPrefHeight(35);
//		for (Item item : itemList) {
//			itemNamesList.add(item.name);
//		}
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
		@SuppressWarnings("unchecked")
		TextFormatter formatter = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> {
		    return pattern.matcher(change.getControlNewText()).matches() ? change : null;
		});
		bidField.setTextFormatter(formatter);
		Button bidButton = new Button("Place bid");
		bidButton.setPrefHeight(35);
		Label bidErrorMessage = new Label();
		bidErrorMessage.setStyle("-fx-text-fill: red");
		
		
		// Divider line between controller and watchlist scrollpane
		Separator divider = new Separator();
		
		
		// Scrollpane + watchlist view nodes
//		ScrollPane scrollWindow = new ScrollPane(); // watchlist scrollpane
//		scrollWindow.setMaxHeight(650);
		ListView<VBox> itemView = new ListView<VBox>();
//		scrollWindow.setContent(itemView);
//		scrollWindow.setPannable(true);
		itemView.setPrefWidth(1890);
		itemView.setPrefHeight(675);

		
		// Final Layout Specification from top to bottom: welcomeMessage, controller, scrollWindow (containing itemView VBox)
		HBox controllerRow1 = new HBox(5, itemMenu, addItemButton, addItemErrorMessage);  // controller node
		controllerRow1.setAlignment(Pos.CENTER);
		HBox controllerRow2 = new HBox(5, bidInstruction, bidField, bidButton, bidErrorMessage);
		controllerRow2.setAlignment(Pos.CENTER);
		VBox controller = new VBox(5, controllerRow1, controllerRow2); // controller node (with all user interface)
		VBox grid = new VBox(5, welcomeMessageRow, controller, divider, itemView);
		VBox.setMargin(itemView, new Insets(10));

		
		// Helper threads to handle window resize spacing, button enabling, and periodically updating the itemList
		primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> { // will resize the welcome message to be centered everytime stage window is resized by user
			Platform.runLater(() -> {
			welcomeMessageRow.setSpacing((primaryStage.getWidth() / 2) - logOutButton.getWidth() - (welcomeMessage.getWidth() / 2));
			});
		});
		Thread enableButtonThread = new Thread(new Runnable () {
			@Override
			public void run() {
				while (!sessionDone) { // while the itemView has nodes added to it, keep queuing commands to update the client's itemList
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
//		Thread itemListUpdaterThread = new Thread(new Runnable () {
//			@Override
//			public void run() {
//				while (!sessionDone) {
//					if (!itemView.getItems().isEmpty()) { // while the itemView has nodes added to it, keep queuing commands to update the client's itemList
//						sendToServer("updateItemList"); // update once per second
//						try { // let other threads do work for 50 ms before updating this client's list
//							Thread.sleep(50);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//		});
		Thread updateWatchlistThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!sessionDone) {
					for (Pair<String, VBox> nodePair : watchlistInfoNodes) {
						String itemName = nodePair.getKey();
						VBox itemNode = nodePair.getValue();
						VBox itemInfoBox = (VBox) itemNode.lookup("#itemInfoBox");
						HBox itemInfoHBox = (HBox) itemInfoBox.getChildren().get(0);
						Label itemBidInfo = (Label) itemInfoHBox.lookup("#itemBidInfo");
						Label itemTimeInfo = (Label) itemInfoHBox.lookup("#itemTimeInfo");
						Label soldInfo = (Label) itemInfoBox.lookup("#soldInfo");
						for (Item item : watchlistItems) {
							if (itemName.contentEquals(item.name)) {
								BigDecimal bigDecimalDuration = item.duration;
								int minutes = bigDecimalDuration.intValue();
								BigDecimal decimalPart = bigDecimalDuration.subtract(new BigDecimal(minutes));
								int seconds = (decimalPart.multiply((new BigDecimal(60)))).intValue();
								String minutesString = String.format("%02d", minutes);
								String secondsString = String.format("%02d", seconds);
								
								Platform.runLater(() -> {
									if (bigDecimalDuration.compareTo(BigDecimal.ZERO) == 0) {
										itemNode.setDisable(true);
									}
									String currentBidString = "$" + MONEY_FORMATTER.format(item.currentBidPrice);
									if (item.currentBidPrice == 0.00) {
										currentBidString = "N/A";
									}
									itemBidInfo.setText("Minimum Bidding Price: $" + MONEY_FORMATTER.format(item.minPrice) + "  Current Bid: " + currentBidString + "  Highest Bidder: " + item.highestBidderUsername);
									if (bigDecimalDuration.compareTo(TEN_SECONDS) == -1) {
										itemTimeInfo.setStyle("-fx-text-fill: red; -fx-font-size: 13px");
									}
									itemTimeInfo.setText("  Time left: " + minutesString + ":" + secondsString);
									if (!item.soldMessage.contentEquals("")) {
										soldInfo.setText(item.soldMessage);
									}
								});
							}
						}
					}
					try { // let other threads run for 50 ms intervals before updating GUI
						Thread.sleep(50);
					} 
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
        });
		enableButtonThread.start();
		enableButtonThread.setName("enableButtonThread");
		activeThreadList.add(enableButtonThread);
		updateWatchlistThread.start();
		updateWatchlistThread.setName("updateWatchlistThread");
		activeThreadList.add(updateWatchlistThread);
		
		// ALL BUTTON HANDLERS ARE BELOW:
		//===========================================================================================================
		addItemButton.setOnAction(e -> { // add-item button handler and use lambda expression instead of EventHandler
			String chosenItemName = itemMenu.getValue();
			if (!watchlistItemNames.contains(chosenItemName)) { // item has not been added to watchlist yet -> add to watchlist
				clearAuctionErrorMessages(addItemErrorMessage, bidErrorMessage);
				Item chosenItem = null;
				for (Item item: itemList) {
					if (item.name.contentEquals(chosenItemName)) chosenItem = item;
				}
				Label itemNameLabel = new Label(chosenItem.name);
				itemNameLabel.setStyle("-fx-text-fill: black; font-weight: bold; -fx-font-size: 18pt");
				Label itemDescription = new Label (chosenItem.description);
				String currentBidPrice = "N/A";
				if (chosenItem.currentBidPrice != 0.00) {
					currentBidPrice = new String(); currentBidPrice += "$" + MONEY_FORMATTER.format(chosenItem.currentBidPrice);
				}
				BigDecimal bigDecimal = chosenItem.duration;
				Integer minutes = bigDecimal.intValue();
				BigDecimal decimalPart = bigDecimal.subtract(new BigDecimal(minutes));
				Integer seconds = (decimalPart.multiply((new BigDecimal(60)))).intValue();
				String minutesString = String.format("%02d", minutes);
				String secondsString = String.format("%02d", seconds);
				Label itemBidInfo = new Label (("Minimum Bidding Price: $" + MONEY_FORMATTER.format(chosenItem.minPrice) + "  Current Bid: " + currentBidPrice + "  Highest Bidder: " + chosenItem.highestBidderUsername));
				itemBidInfo.setId("itemBidInfo");
				Label itemTimeInfo = new Label("  Time left: " + minutesString + ":" + secondsString);
				itemTimeInfo.setId("itemTimeInfo");
				HBox itemInfoHBox = new HBox(0, itemBidInfo, itemTimeInfo);
				Label soldInfo = new Label(chosenItem.soldMessage);
				soldInfo.setFont(new Font("Segoe UI Bold", 12));
				soldInfo.setId("soldInfo");				
				VBox itemInfoBox = new VBox(0, itemInfoHBox, soldInfo);
				itemInfoBox.setId("itemInfoBox");
				VBox.setMargin(soldInfo, new Insets(2, 0, 2, 0));
				Separator itemDivider = new Separator();
				VBox itemNode = new VBox(2, itemNameLabel, itemDescription, itemInfoBox, itemDivider);
				itemView.getItems().add(itemNode);
				watchlistItemNames.add(chosenItemName);
				watchlistItems.add(chosenItem);
				watchlistInfoNodes.add(new Pair<String, VBox>(chosenItemName, itemNode));
//				scrollWindow.setViewportBounds((itemView.getBoundsInParent()));
//	    		observableItemList.addListener((ListChangeListener<? super Item>) itemNode);
			}
			else { // else, item is already in watchlist window
				addItemErrorMessage.setText("ERROR! " + chosenItemName + " is already added to the watchlist.");
			}
		});
		bidButton.setOnAction(e -> { // palce-bid button handler with lambda expression
			String chosenItemName = itemMenu.getValue();
			Double userBid = Double.parseDouble(bidField.getText());
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
				else if (chosenItem.duration.doubleValue() <= 0.00) {
					bidErrorMessage.setText("INVALID BID! Auction for this item has closed.");
				}
				else { // userBid is a valid bid
					clearAuctionErrorMessages(addItemErrorMessage, bidErrorMessage);
//					isItemListChanged = false;
					sendToServer("updateBidPrice|" + chosenItemName + "|" + String.valueOf(userBid) + "|" + username);
					bidErrorMessage.setText("BID SUCCESSFUL! You are now the highest bidder.");
					bidField.clear();
				}
		});
		logOutButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// try to set flags so that all the helper threads finish executing
				sessionDone = true;
				for (Thread t : activeThreadList) {
//					System.out.println("Auction background thread name: " + t.getName());
				    try {
						t.join();
					} catch (InterruptedException e) {
						System.out.println("Java FX Application Thread interrupted while waiting for background threads to join.");
					}
				}
				
				resetAllVariables();
				try {
					closeSocketConnection();
				} catch (IOException e) {
					System.out.println("IOException when trying to close input/output streams and socket.");
					e.printStackTrace();
				}
				
				primaryStage.setTitle("Auction Login Page"); // Set the stage title 
				primaryStage.setScene(generateNewLoginScene(primaryStage)); // Place the scene in the stage
				primaryStage.show(); // Display the stage 
			}
		});
		
		
		
		
		// create and return auction Scene
		Scene auctionScene = new Scene(grid, MAX_WIDTH, MAX_HEIGHT);
		return auctionScene;
	}
	
	
	// helper method that clears all error messages in the auction screen GUI
	// usage: call this method after every successful user button press
	private static void clearAuctionErrorMessages(Label addItemErrorMessage, Label bidErrorMessage) {
		addItemErrorMessage.setText("");
		bidErrorMessage.setText("");
	}
	
  
	// helper method that sets up connection to server through socket 4242, given host IP address
	// usage: only call this method after successful sign-in button-press
	private static void setUpSocketConnection(String hostIP) throws Exception {
	    try {
	    	socket = new Socket(hostIP, 4242);
	    } catch (IOException e) {
	    	System.out.println("Error generating socket to server.");
	    }
	    
	    System.out.println("Connecting to server... " + socket);
	    toServer = new PrintWriter(socket.getOutputStream());
	    fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    
	    // readerThread handles all communication from the server
	    Thread readerThread = new Thread(new Runnable() {
	    	@Override
	    	public void run() {
	    		String input;
	    		try {
	    			while (!sessionDone && !socket.isClosed()) { // original while-loop: ((input = fromServer.readLine()) != null)
	    				if (fromServer.ready()) {
	    					input = fromServer.readLine();
//	    					System.out.println("readerThread got following command from server: " + input); // uncomment this line to see commands recieved from server
	    					processRequest(input);
	    				}
	    			}
	    		} catch (Exception e) {
	    			System.out.println("readerThread threw an Exception when getting input from the 'fromServer' stream.");
	    			e.printStackTrace();
	    		}
	    	}
	    });
	    
	    // writerThread handles writes all client commands to the server
//	    Thread writerThread = new Thread(new Runnable() {
//	    	@Override
//	        public synchronized void run() {
//	    		while (true) {
//	        	    if (!commandBuffer.isEmpty()) {
//		    			String topCommand = commandBuffer.remove();
////		            	GsonBuilder builder = new GsonBuilder();
////		            	Gson gson = builder.create();
//		    			sendToServer(topCommand);
//	        	    }
//	    		}
//	        }
//	    });
	    readerThread.start();
	    readerThread.setName("readerThread");
	    activeThreadList.add(readerThread);
//	    writerThread.start();
	}
	
	private static void closeSocketConnection() throws IOException {
		Client.sendToServer("removeObserver");

	}
	
	
	// format of messages returned from server will be: <command+"Successful">|<inputDataString>
	// command and each part of data is separated by |
    protected static void processRequest(String input) {
    	String[] inputArr = input.split("\\|"); // split input string around pipe character
    	
    	// TODO: use inputArr[0] to decide how to use the information from the server below:
    	switch (inputArr[0]) {
    		case "initializeItemListSuccessful":
    			itemList.clear();
 				String name = "";
				String description = "";
				Double minPrice = 0.00;
				Double currentBidPrice = 0.00;
				String highestBidderUsername = "";
				String soldMsg = "";
				BigDecimal duration = null;
    			for (int i = 1; i < inputArr.length; i++) {
    				if (!inputArr[i].contentEquals("")) {
	    				if (i % 7 == 1) {
	    					name += inputArr[i];
	    					itemNamesList.add(name);
	    				}
	    				else if (i % 7 == 2) {
	    					description += inputArr[i];
	    				}
	    				else if (i % 7 == 3) {
	    					minPrice = Double.parseDouble(inputArr[i]);
	    				}
	    				else if (i % 7 == 4) {
	    					currentBidPrice = Double.parseDouble(inputArr[i]);
	    				}
	    				else if (i % 7 == 5) {
	    					highestBidderUsername = inputArr[i];
	    				}
	    				else if (i % 7 == 6) {
	    					duration = new BigDecimal(inputArr[i]);
	    				}
	    				else {
	    					soldMsg = inputArr[i];
	    	 				itemList.add(new Item(name, description, minPrice, currentBidPrice, highestBidderUsername, duration, soldMsg));
	    	 				name = "";
	        				description = "";
	        				highestBidderUsername = "";
	        				soldMsg = "";
	    				}
    				}
    			}
    			isItemListUpdated = true; // set changed flag
    			break;
    		case "updateBidPriceSuccessful": // server message in the form: updateBidPriceSuccessful|<itemName>|<newBidPrice>|<newHighestBidderUsername>
    			String itemNameToUpdate = inputArr[1];
    			Double newCurrentBidPrice = Double.parseDouble(inputArr[2]);
    			String newHighestBidderUsername = inputArr[3];
    			for (Item item : itemList) {
    				if (item.name.contentEquals(itemNameToUpdate)) {
    					item.currentBidPrice = newCurrentBidPrice;
    					item.highestBidderUsername = newHighestBidderUsername;
    					break;
    				}
    			}
    			break;
    		case "updateDurationSuccessful": // server message in the form: notifyItemSoldSuccessful|<itemName>|<newDuration>
    			String itemName = inputArr[1];
    			BigDecimal newDuration = new BigDecimal(inputArr[2]);
    			for (Item item : itemList) {
    				if (item.name.contentEquals(itemName)) {
    					item.duration = newDuration;
    					break;
    				}
    			}
    			break;
    		case "notifyItemSoldSuccessful": // server message in the form: notifyItemSoldSuccessful|<itemName>|<soldMessage>
    			String itemNameToNotify = inputArr[1];
    			String soldMessage = inputArr[2];
    			for (Item item : itemList) {
    				if (item.name.contentEquals(itemNameToNotify)) {
    					item.soldMessage = soldMessage;
        				break;
    				}
    			}
    			// TODO: ADD soldMessage to a queue to be updated by a new thread that periodically writes messages to a live alert console
    			break;
    		
    	}//end of switch
    	
    	return;
    }

  
	public static void main(String[] args) {	
		// Launch Java FX Application thread
		launch(args);
	}
	
	
    protected static void sendToServer(String string) {
    	System.out.println("Sending to server: " + string); // uncomment this to see commands sent to the server
    	toServer.println(string);
    	toServer.flush();
    }

}