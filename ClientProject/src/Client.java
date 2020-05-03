/*  Client.java
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */


import java.util.Scanner;

import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Application {

	private static String host = "127.0.0.1"; // default local host
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Scanner consoleInput = new Scanner(System.in);
	boolean isFirstClickOfLoginField;
	boolean isFirstClickOfPasswordField;
	
	public Client() {
		host = "127.0.0.1"; // default local host (later change this to be able to changed based on input from login menu)
		consoleInput = new Scanner(System.in); // TODO: input will not be from console, change to get from GUI buttons
		boolean isFirstClickOfLoginField = true;
		boolean isFirstClickOfPasswordField = true;
	}

  
	@Override
	public void start(Stage primaryStage) {
		BorderPane loginPage = new BorderPane();
		
		// Initialize nodes for top of loginPage borderPane
		Label greeting = new Label();
		Label signIn = new Label();
		greeting.setText("Welcome to Ebae!");
		greeting.setFont(new Font("Segoe UI Bold", 24));
		greeting.setAlignment(Pos.CENTER); 
		signIn.setText("Sign in to continue:");
		signIn.setFont(new Font("Segoe UI", 16));
		signIn.setAlignment(Pos.CENTER); 
		VBox welcomeBox = new VBox(2, greeting, signIn);
		welcomeBox.setAlignment(Pos.CENTER); 
		loginPage.setTop(welcomeBox);
		
		// Initialize nodes for center of loginPage borderPane
		TextField loginField = new TextField(); // login field node
		
		loginField.setText("Email or username");
		loginField.setStyle("-fx-text-fill: grey; font-style: italic");
		loginField.setFont(new Font("Segoe UI", 12));
		loginField.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) { // wipe the gray text once user clicks login textfield
				if (isFirstClickOfLoginField) {
					System.out.println("field clicked.");
					loginField.clear();
				}
				isFirstClickOfLoginField = false;
			}
		});
		TextField passwordField = new TextField(); // password field node
		passwordField.setText("Password");
		passwordField.setStyle("-fx-text-fill: grey; font-style: italic");
		passwordField.setFont(new Font("Segoe UI", 12));
		passwordField.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) { // wipe the gray text once user clicks login textfield
				if (isFirstClickOfPasswordField) {
					System.out.println("field clicked.");
					passwordField.clear();
				}
				isFirstClickOfPasswordField = false;
			}
		});
		Button signInButton = new Button("Sign in"); // sign-in button node
		signInButton.setStyle("-fx-text-fill: blue; font-style: bold; -fx-font-size: 16px");
		signInButton.setAlignment(Pos.CENTER); 
		VBox credentialsBox = new VBox(5, loginField, passwordField, signInButton);
		loginPage.setCenter(credentialsBox);
		
		
		// Create scene and place on the stage
		Scene scene = new Scene(loginPage, 450, 300);
		primaryStage.setTitle("Auction Login Page"); // Set the stage title 
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage 
  
		// set up this client's socket connection with server
		try {
			new Client().setUpSocketConnection();
		} catch (Exception e) {
			System.out.println("exception when setting up socket connection");
			e.printStackTrace();
		}
	}
  
	// set up connection to server through socket 4242
	private void setUpSocketConnection() throws Exception {
	    @SuppressWarnings("resource")
	    Socket socket = new Socket(host, 4242);
	    System.out.println("Connecting to server... " + socket);
	    fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    toServer = new PrintWriter(socket.getOutputStream());
	
	    Thread readerThread = new Thread(new Runnable() {
	    	@Override
	    	public void run() {
	    		String input;
	    		try {
	    			while ((input = fromServer.readLine()) != null) {
	    				System.out.println("From server: " + input);
	    				processRequest(input);
	    			}
	    		} catch (Exception e) {
	    		e.printStackTrace();
	    		}
	    	}
	    });

	    Thread writerThread = new Thread(new Runnable() {
	    	@Override
	        public void run() {
	          while (true) {
	            String input = consoleInput.nextLine();
//	            GsonBuilder builder = new GsonBuilder();
//	            Gson gson = builder.create();
	            sendToServer(input);
	          }
	        }
	    });

	    readerThread.start();
	    writerThread.start();
	}

    protected void processRequest(String input) {
      return;
    }

    protected void sendToServer(String string) {
      System.out.println("Sending to server: " + string);
      toServer.println(string);
      toServer.flush();
    }
  
	public static void main(String[] args) {
		launch(args);
	}

}