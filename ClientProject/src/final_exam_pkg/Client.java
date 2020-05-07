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
	// inner Item class definition
	static class Item {
		private String name;
		private String description;
		private double minPrice;
		private double currentBidPrice;
		private String highestBidderUsername;
		private BigDecimal duration;
		private String soldMessage;
		
		// new parameterized constructor - Items should only be instiantiated by calling this constructor 
		private Item (String name, String description, double minPrice, double currentBidPrice, String highestBidderUsername, BigDecimal duration, String soldMsg) {
			this.name = name;
			this.description = description;
			this.minPrice = minPrice;
			this.currentBidPrice = currentBidPrice;
			this.highestBidderUsername = new String(); this.highestBidderUsername += highestBidderUsername;
			this.duration = duration;
			this.soldMessage = soldMsg;
		}
	}
	
	// constant data fields
	private static final double MAX_WIDTH = Screen.getPrimary().getBounds().getMaxX();
	private static final double MAX_HEIGHT = Screen.getPrimary().getBounds().getMaxY();
	private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("#.##");
	private static final BigDecimal TEN_SECONDS = new BigDecimal(0.175);
	
	// networking data fields
	private static Socket socket;
	private static BufferedReader fromServer;
	private static PrintWriter toServer;
	
	//
	private static boolean isIPFieldCleared = false;
	private static String username = null;
	private static ArrayList<Item> itemList = new ArrayList<Item> (); // look into OberservableArrayLists if u can figure out listeners
	private static ArrayList<String> itemNamesList = new ArrayList<String> ();
	private static HashSet<String> watchlistItemNames = new HashSet<String>();
	private static ArrayList<Item> watchlistItems = new ArrayList<Item>();
	private static ArrayList<Pair<String, VBox>> watchlistInfoNodes = new ArrayList<Pair<String, VBox>>();
	private static boolean isItemListUpdated = false;
	private static boolean sessionDone = false;
	private static ArrayList<Thread> activeThreadList = new ArrayList<Thread>();
	
	
	/**
	 * Resets all of the Client class's non-networking data members to default values. 
	 * Call this method when user presses the auction "log-out" button.
	 * 
	 * @return void
	*/
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
	
	
	// default constructor (not called in this program since only 1 client exists per project)
	public Client() {
		// if changing implementation to have instances of Client, initialize data members here
	}
	
	
	public static void main(String[] args) {	
		launch(args); // Launch Java FX Application thread
	}
	
	@Override
	public void start(Stage primaryStage) {
		// Generate initial login scene and place it on the primary stage
		primaryStage.setTitle("Auction Login Page"); // Set the stage title 
		primaryStage.setScene(generateNewLoginScene(primaryStage)); // Place the scene in the stage
		primaryStage.show(); // Display the stage 
	}
	
	
	/**
	 *  Helper function for generateNewLoginScene() that returns a regular expression for generating the ip field text-formatter. Remember to cite this from StackOverflow.
	 *  Used on line 182-184 in the ipFilter TextFormatter definition.
	 *  
	 *  @return String represesenting a regular expression to filter certain user textfield input
	 */
    private static String getIPRegExpression() {
        String firstBlock = "(([01]?[0-9]{0,2})|(2[0-4][0-9])|(25[0-5]))" ;
        String nextBlock = "(\\." + firstBlock + ")" ;
        String ipAddress = firstBlock+ "?" + nextBlock + "{0,3}";
        return "^" + ipAddress;
    }
	
    
    
    // LOGIN PAGE LOGIC BELOW
    // ==========================================================================================
    /**
     *  Initializes all nodes in the login page and returns this new login scene. 
     *  All buttons, textfields, and other dynamic nodes are reset to the initial state.
     *  
     *  @param primaryStage paramater of start()
     *  @return Scene loginScene
     */
	private static Scene generateNewLoginScene(Stage primaryStage) {
		// All nodes in this scene placed on this borderpane
		BorderPane loginPane = new BorderPane();
			
		// borderPane RIGHT nodes (quit button)
		Button quitButton = new Button("Quit"); // quit button node
		quitButton.setPrefSize(70,  30);
		quitButton.setStyle("-fx-text-fill: red; font-weight: bold; -fx-font-size: 16px");
		loginPane.setRight(quitButton);
		BorderPane.setMargin(quitButton,  new Insets(10, 10, 0, 0));
		
		
		// borderPane CENTER-TOP nodes (welcome and sign-in text)
		Label greeting = new Label();
		Label signIn = new Label();
		greeting.setText("Welcome to Ebae!");
		greeting.setFont(new Font("Segoe UI Bold", 24));
		signIn.setText("Sign in to continue:");
		signIn.setFont(new Font("Segoe UI", 16));
		VBox welcomeBox = new VBox(2, greeting, signIn);
		welcomeBox.setAlignment(Pos.CENTER); 
		VBox.setMargin(greeting,  new Insets(10, 0, 0, 0));
		
		
		// borderPane CENTER nodes (All textfields and sign-in buttons)
		TextField hostIPField = new TextField(); // IP field node
		hostIPField.setText("127.0.0.1"); // set the server ip address to be local machine by default
		hostIPField.setStyle("-fx-text-fill: grey; font-style: italic");
		hostIPField.setFont(new Font("Segoe UI", 12));
		String regEx = getIPRegExpression();
		final UnaryOperator<TextFormatter.Change> ipFilter = e -> {
			return (e.getControlNewText().matches(regEx) ? e : null);
		};
		hostIPField.setOnMousePressed(e -> { // wipes the ip field on first click
				if (!isIPFieldCleared) {
					hostIPField.clear();
					hostIPField.setTextFormatter(new TextFormatter<>(ipFilter));
					isIPFieldCleared = true;
				}
		});
		TextField loginField = new TextField(); // login field
		loginField.setPromptText("Email or username");
		loginField.setStyle("-fx-text-fill: grey; font-style: italic");
		loginField.setFont(new Font("Segoe UI", 12));
		TextField passwordField = new TextField(); // password field
		passwordField.setPromptText("Password");
		passwordField.setStyle("-fx-text-fill: grey; font-style: italic");
		passwordField.setFont(new Font("Segoe UI", 12));
		Button signInButton = new Button("Sign in"); // sign-in button
		signInButton.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 16px");
		signInButton.setAlignment(Pos.CENTER); 
		Label signInErrorMsg = new Label(); // sign-in error msg
		signInErrorMsg.setStyle("-fx-text-fill: red");
		HBox signInBox = new HBox (10, signInButton, signInErrorMsg); // place error message to the right of sign-in button
		Button guestButton = new Button("Continue as guest"); // sign-in as guest button
		
		
		// BorderPane Center Node's Alignment: Login field, password field, sign in button, and guest button
		VBox centerBox = new VBox(5, welcomeBox, hostIPField, loginField, passwordField, signInBox, guestButton);
		centerBox.setMaxSize(400, 900); // this controls the max size of the text fields
		VBox.setMargin(welcomeBox, new Insets(0, 10, 15, 10));
		VBox.setMargin(hostIPField, new Insets(0, 10, 0, 10));
		VBox.setMargin(loginField, new Insets(0, 10, 0, 10));
		VBox.setMargin(passwordField, new Insets(0, 10, 0, 10));
		VBox.setMargin(signInBox, new Insets(0, 10, 0, 10));
		VBox.setMargin(guestButton, new Insets(15, 10, 0, 10));
		loginPane.setCenter(centerBox);
		
		
		// BUTTON HANDLERS DEFINITIONS BELOW
		// ==================================================================
		// sign-in button handler
		signInButton.setOnAction(new EventHandler<ActionEvent>() { 
			@Override
			public synchronized void handle(ActionEvent event) {
				String inputUsername = loginField.getText();
				String password = passwordField.getText();
				String hostIP = hostIPField.getText();

				if (inputUsername.equals("")) {
					signInErrorMsg.setText("ERROR! Please enter a username");
					// TODO: write helper method clearAllLoginFields(TextField loginField, TextField passwordField, TextField hostIPField) that clears all of the 3 textfields and call method here
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else if (password.equals("")) {
					signInErrorMsg.setText("ERROR! Please enter a password");
					// TODO: call clearAllLoginFields(loginField, passwordField, hostIPField);
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else if (hostIP.contentEquals("")) {
					signInErrorMsg.setText("ERROR! Please enter a valid host IP address");
					// TODO: clearAllLoginFields(loginField, passwordField, hostIPField);
					loginField.setText("");
					passwordField.setText("");
					hostIPField.setText("");
				}
				else { // else, none of the text-fields are empty		
					try {
						signInErrorMsg.setText("");
			    		setUpSocketConnection(hostIP);
			    		hostIP = "";
			    		sendToServer("initializeItemList"); // initialize Item menu as first command
			    		while (!isItemListUpdated) {System.out.println("Loading items from server database..."); Thread.yield();} // wait until client item database is initialized before continuing
			    		username = inputUsername;
			    		primaryStage.setTitle("Auction Site"); 
						primaryStage.setScene(generateNewAuctionScene(primaryStage)); 
						primaryStage.setMaximized(true);
						primaryStage.show();
					} catch (Exception e) {
						System.out.println("ERROR! Exception thrown since Server IP address is invalid.");
						signInErrorMsg.setText("ERROR! Server refused to connect.");
						// TODO: clearAllLoginFields(loginField, passwordField, hostIPField);
						loginField.setText("");
						passwordField.setText("");
						hostIPField.setText("");
					}
				}
			}
    	});
		// guest button-handler (if no username and no password filled in)
		guestButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public synchronized void handle(ActionEvent event) {
				String hostIP = hostIPField.getText();

				if (hostIP.contentEquals("")) {
					signInErrorMsg.setText("ERROR! Please enter a valid host IP address");
					// TODO: clearAllLoginFields(loginField, passwordField, hostIPField);
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
						primaryStage.setMaximized(true);
						primaryStage.show();
					} catch (Exception e) {
						signInErrorMsg.setText("ERROR! Server refused to connect.");
						// TODO: clearAllLoginFields(loginField, passwordField, hostIPField);
						loginField.setText("");
						passwordField.setText("");
						hostIPField.setText("");
					}
				}
			}
    	});
		// quit button-handler (exits application and program)
		quitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Platform.exit();
				System.exit(0);
			}
    	});
		
		
		// generate new login scene and return it
		Scene loginScene = new Scene(loginPane, 425, 325);
		return loginScene;
	}
	
	
	
	
	// AUCTION PAGE LOGIC BELOW
	// =======================================================================================================
	/**
	 * Generate all FX nodes in the auction page and return this newly generated Scene.
	 * Call this method right after the user signs-in successfully (either through sign-in handler or guest handler).
	 * 
	 * @param primaryStage
	 * @return auctionScene, a newly generated auctionScene with freshly initialized nodes
	 */
	private static Scene generateNewAuctionScene(Stage primaryStage) {
		// WELCOME MESSAGE ROW NODES: Welcome header + log-out button
		Label welcomeMessage = new Label("Welcome to the auction, " + Client.username + "!"); // welcome msg
		welcomeMessage.setStyle("-fx-text-fill: blue; font-weight: bold; -fx-font-size: 24pt");
		Button logOutButton = new Button("Log out"); // log-out button
		logOutButton.setStyle("-fx-text-fill: red; font-weight: bold; -fx-font-size: 16px");
		logOutButton.setAlignment(Pos.CENTER_RIGHT);
		HBox welcomeMessageRow = new HBox(640, welcomeMessage, logOutButton);
		welcomeMessageRow.setAlignment(Pos.CENTER_RIGHT);
		HBox.setMargin(logOutButton, new Insets(10, 10, 0, 0));
		HBox.setMargin(welcomeMessage, new Insets(10, 0, 0, 0));
		
		
		// CHANGE LISTENER will resize the welcome header spacing if the stage window gets resized by user:
		primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
			// TODO: verify if we can take out the Platform.runLater() since changelisteners are already on the FX thread
			Platform.runLater(() -> {
				welcomeMessageRow.setSpacing((primaryStage.getWidth() / 2) - logOutButton.getWidth() - (welcomeMessage.getWidth() / 2));
			});
		});
		
		
		// CONTROLLER ROW 1 NODES: Drop-down menu + addItem button + addItemErrorMessage
		ChoiceBox<String> itemMenu = new ChoiceBox<String>(); // item menu node
		itemMenu.setPrefHeight(35);
		itemMenu.getItems().addAll(itemNamesList);
		Button addItemButton = new Button("Add this item to watchlist"); 
		addItemButton.setPrefHeight(35);
		Label addItemErrorMessage = new Label();
		addItemErrorMessage.setStyle("-fx-text-fill: red");
		HBox controllerRow1 = new HBox(5, itemMenu, addItemButton, addItemErrorMessage);  // controller node
		controllerRow1.setAlignment(Pos.CENTER);
		
		
		// CONTROLLER ROW 2 NODES: enter bid label + bid textfield + bid button
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
		HBox controllerRow2 = new HBox(5, bidInstruction, bidField, bidButton, bidErrorMessage);
		controllerRow2.setAlignment(Pos.CENTER);
		
		
		// CONTROLLER: controller row 1 + controller row 2
		VBox controller = new VBox(5, controllerRow1, controllerRow2); // controller node (with all user interface)
		
		
		// Divider line between controller and itemView listview
		Separator divider = new Separator();
		
		
		// Listview = watchlist window
		ListView<VBox> itemView = new ListView<VBox>();
		itemView.setPrefWidth(1890);
		itemView.setPrefHeight(675);

		// TODO: Add nodes for the multi-tab console (showing sell history and bid history) below:
		
		
		
		// Final Layout VBox: welcomeMessageRow, controller, itemView (=watchlist window) // TODO: add historyConsole node to grid
		VBox grid = new VBox(5, welcomeMessageRow, controller, divider, itemView);
		VBox.setMargin(itemView, new Insets(10));

		
		// BACKGROUND THREADS to handle window resize spacing, button enabling, and periodically updating the itemList:
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
		enableButtonThread.setName("enableButtonThread");
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
									String currentBidString = "$" + MONEY_FORMATTER.format(item.currentBidPrice);
									if (item.currentBidPrice == 0.00) {
										currentBidString = "N/A";
									}
									itemBidInfo.setText("Minimum Bidding Price: $" + MONEY_FORMATTER.format(item.minPrice) + "  Current Bid: " + currentBidString + "  Highest Bidder: " + item.highestBidderUsername);
									if (bigDecimalDuration.compareTo(TEN_SECONDS) == -1) {
										itemTimeInfo.setStyle("-fx-text-fill: red; -fx-font-size: 13px");
									}
									itemTimeInfo.setText("  Time left: " + minutesString + ":" + secondsString);
									if (!item.soldMessage.contentEquals("Item is up for sale")) {
										soldInfo.setText(item.soldMessage);
									}
									if (bigDecimalDuration.compareTo(BigDecimal.ZERO) == 0) {
										itemNode.setDisable(true);
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
		updateWatchlistThread.setName("updateWatchlistThread");
		enableButtonThread.start();
		activeThreadList.add(enableButtonThread);
		updateWatchlistThread.start();
		activeThreadList.add(updateWatchlistThread);
		
		
		// ALL BUTTON HANDLERS ARE BELOW:
		//===========================================================================================================
		// add item button-handler
		addItemButton.setOnAction(new EventHandler<ActionEvent>() { // add-item button handler and use lambda expression instead of EventHandler
			@Override
			public void handle(ActionEvent event) {
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
				}
				else { // else, item is already in watchlist window
					addItemErrorMessage.setText("ERROR! " + chosenItemName + " is already added to the watchlist.");
				}
			}
		});
		// bid button handler
		bidButton.setOnAction(new EventHandler<ActionEvent>() { // palce-bid button handler with lambda expression
			@Override
			public void handle(ActionEvent event) {
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
			}
		});
		// log out button-handler
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
		
		
		// create and return newly initialized auction Scene
		Scene auctionScene = new Scene(grid, MAX_WIDTH, MAX_HEIGHT);
		return auctionScene;
	}
	
	
	/**
	 * Helper method for generateNewAuctionScene() that clears all error messages in the auction GUI.
	 * Call this method after every user button press in the auction scene.
	 * 
	 * @param addItemErrorMessage, Label from the auctionScene
	 * @param bidErrorMessage, Label from the auctionScene
	 */
	private static void clearAuctionErrorMessages(Label addItemErrorMessage, Label bidErrorMessage) {
		addItemErrorMessage.setText("");
		bidErrorMessage.setText("");
	}
	
	
	/**
	 * Sets up the socket connection on port 4242 to the IP address specified by hostIP parameter as well as the "toServer" and "fromServer" IO streams to communicate with server.
	 * Also declares and starts a readerThread that handles all commands from the server.
	 * Usage: call this method after a successful sign-in, in the signIn button handler.
	 * 
	 * @param hostIP, String that represents the host server IP address that this client is trying to connect to
	 * @throws Exception
	 */
	private static void setUpSocketConnection(String hostIP) throws Exception {
		// set up socket connection and data streams
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
	    readerThread.setName("readerThread");
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
//	    writerThread.setName("writerThread");
//	    writerThread.start();
//	    activeThreadList.add(writerThread);
	    
	    readerThread.start();
	    activeThreadList.add(readerThread);
	}
	
	
	/**
	 * Sends "removeObserver" command to server to tell the server to remove this client's clientHandler from the observer list.
	 * Server will also close the socket-connection from the server side. Called when user presses logOut button.
	 * 
	 * @throws IOException
	 */
	private static void closeSocketConnection() throws IOException {
		Client.sendToServer("removeObserver");
	}
	
	
	/**
	 * Sends a command string to the server through the toServer PrintWriter output stream. 
	 * Commands you should pass in should be in the following list: "initializeItemList" , "removeObserver" , "updateBidPrice"
	 * 
	 * @param string, represents the command to send to the server
	 */
    protected static void sendToServer(String string) {
    	System.out.println("Sending to server: " + string); // uncomment this to see commands sent to the server
    	toServer.println(string);
    	toServer.flush();
    }
    
	
	/**
	 * Processes all input commands from the server by parsing and using the split input to update the client side itemList database.
	 * Called within the readerThread run() method while the session is ongoing and the client socket is not closed.
	 * COMMUNICATION PROTOCOL: format of messages returned from server: <command+Successful>|<dataFromServer>
     * Note that the different sections of information are separated by PIPE characters, not spaces.
	 * 
	 * @param input, represents the input String from the server
	 */
    protected static void processRequest(String input) {
    	// Split string around the pipe character as a delimiter
    	String[] inputArr = input.split("\\|"); 
    	
    	// The first argument of the input string should always be <commandSuccessful>
    	// Therefore, choose action to take based on value of inputArr[0]
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
    
} // end of Client class